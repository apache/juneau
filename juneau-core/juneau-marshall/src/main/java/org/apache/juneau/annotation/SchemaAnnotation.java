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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.jsonschema.SchemaUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Schema @Schema} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class SchemaAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Schema DEFAULT = create().build();

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
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Schema a) {
		return a == null || DEFAULT.equals(a);
	}

	/**
	 * Converts the specified <ja>@Schema</ja> annotation into a generic map.
	 *
	 * @param a The annotation instance.  Can be <jk>null</jk>.
	 * @return The schema converted to a map, or and empty map if the annotation was null.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonMap asMap(Schema a) throws ParseException {
		if (a == null)
			return JsonMap.EMPTY_MAP;
		JsonMap m = new JsonMap();
		if (SchemaAnnotation.empty(a))
			return m;
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
		return m
			.appendIf(nem, "additionalProperties", parseMap(a.additionalProperties()))
			.appendIf(ne, "allOf", joinnl(a.allOf()))
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a._default(), a.df()))
			.appendIf(ne, "discriminator", a.discriminator())
			.appendIf(ne, "description", joinnl(a.description(), a.d()))
			.appendFirst(nec, "enum", parseSet(a._enum()), parseSet(a.e()))
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendIf(nem, "externalDocs", ExternalDocsAnnotation.merge(m.getMap("externalDocs"), a.externalDocs()))
			.appendFirst(ne, "format", a.format(), a.f())
			.appendIf(ne, "ignore", a.ignore() ? "true" : null)
			.appendIf(nem, "items", merge(m.getMap("items"), a.items()))
			.appendFirst(ne, "maximum", a.maximum(), a.max())
			.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
			.appendFirst(nm1, "maxProperties", a.maxProperties(), a.maxp())
			.appendFirst(ne, "minimum", a.minimum(), a.min())
			.appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl())
			.appendFirst(nm1, "minProperties", a.minProperties(), a.minp())
			.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
			.appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendIf(nem, "properties", parseMap(a.properties()))
			.appendIf(nf, "readOnly", a.readOnly() || a.ro())
			.appendIf(nf, "required", a.required() || a.r())
			.appendIf(ne, "title", a.title())
			.appendFirst(ne, "type", a.type(), a.t())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendIf(ne, "xml", joinnl(a.xml()))
			.appendIf(ne, "$ref", a.$ref())
		;
	}

	private static JsonMap merge(JsonMap m, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return m;
		Predicate<String> ne = StringUtils::isNotEmpty;
		Predicate<Collection<?>> nec = CollectionUtils::isNotEmpty;
		Predicate<Map<?,?>> nem = CollectionUtils::isNotEmpty;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = ObjectUtils::isNotMinusOne;
		return m
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a._default(), a.df()))
			.appendFirst(nec, "enum", parseSet(a._enum()), parseSet(a.e()))
			.appendFirst(ne, "format", a.format(), a.f())
			.appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax())
			.appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendIf(nem, "items", SubItemsAnnotation.merge(m.getMap("items"), a.items()))
			.appendFirst(ne, "maximum", a.maximum(), a.max())
			.appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl())
			.appendFirst(ne, "minimum", a.minimum(), a.min())
			.appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl())
			.appendFirst(ne, "multipleOf", a.multipleOf(), a.mo())
			.appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendFirst(ne, "type", a.type(), a.t())
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
	public static class Builder extends TargetedAnnotationTMFBuilder {

		boolean aev, allowEmptyValue, emax, emin, exclusiveMaximum, exclusiveMinimum, ignore, r, readOnly, required, ro, sie, skipIfEmpty, ui, uniqueItems;
		ExternalDocs externalDocs=ExternalDocsAnnotation.DEFAULT;
		Items items=ItemsAnnotation.DEFAULT;
		long maxi=-1, maxItems=-1, maxl=-1, maxLength=-1, maxp=-1, maxProperties=-1, mini=-1, minItems=-1, minl=-1, minLength=-1, minp=-1, minProperties=-1;
		String $ref="", cf="", collectionFormat="", discriminator="", f="", format="", max="", maximum="", min="", minimum="", mo="", multipleOf="", p="", pattern="", t="", title="", type="";
		String[] _default={}, _enum={}, additionalProperties={}, allOf={}, d={}, description={}, df={}, e={}, properties={}, value={}, xml={};

		/**
		 * Constructor.
		 */
		protected Builder() {
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
		 * @return This object.
		 */
		public Builder _default(String...value) {
			this._default = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#_enum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder _enum(String...value) {
			this._enum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#$ref} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder $ref(String value) {
			this.$ref = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#additionalProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder additionalProperties(String...value) {
			this.additionalProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#allOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allOf(String...value) {
			this.allOf = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#aev} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder aev(boolean value) {
			this.aev = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#allowEmptyValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowEmptyValue(boolean value) {
			this.allowEmptyValue = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			this.cf = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			this.collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder d(String...value) {
			this.d = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder df(String...value) {
			this.df = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#discriminator} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder discriminator(String value) {
			this.discriminator = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder e(String...value) {
			this.e = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emax(boolean value) {
			this.emax = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emin(boolean value) {
			this.emin = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#exclusiveMaximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(boolean value) {
			this.exclusiveMaximum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#exclusiveMinimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(boolean value) {
			this.exclusiveMinimum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#externalDocs} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder externalDocs(ExternalDocs value) {
			this.externalDocs = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			this.f = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ignore} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ignore(boolean value) {
			this.ignore = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder items(Items value) {
			this.items = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder max(String value) {
			this.max = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(long value) {
			this.maxi = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maximum(String value) {
			this.maximum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxItems(long value) {
			this.maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(long value) {
			this.maxl = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxLength(long value) {
			this.maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxp} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxp(long value) {
			this.maxp = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxProperties(long value) {
			this.maxProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder min(String value) {
			this.min = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mini(long value) {
			this.mini = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minimum(String value) {
			this.minimum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minItems(long value) {
			this.minItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minl(long value) {
			this.minl = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minLength(long value) {
			this.minLength = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minp} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minp(long value) {
			this.minp = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minProperties(long value) {
			this.minProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mo(String value) {
			this.mo = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multipleOf(String value) {
			this.multipleOf = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			this.p = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder pattern(String value) {
			this.pattern = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#properties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder properties(String...value) {
			this.properties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#r} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder r(boolean value) {
			this.r = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#readOnly} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder readOnly(boolean value) {
			this.readOnly = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#required} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder required(boolean value) {
			this.required = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ro} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(boolean value) {
			this.ro = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#sie} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder sie(boolean value) {
			this.sie = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#skipIfEmpty} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder skipIfEmpty(boolean value) {
			this.skipIfEmpty = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			this.t = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#title} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder title(String value) {
			this.title = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(String value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ui(boolean value) {
			this.ui = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uniqueItems(boolean value) {
			this.uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#xml} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder xml(String...value) {
			this.xml = value;
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

	private static class Impl extends TargetedAnnotationTImpl implements Schema {

		private final boolean aev, allowEmptyValue, exclusiveMaximum, emax, exclusiveMinimum, emin, uniqueItems, ui, required, r, readOnly, ro, sie, skipIfEmpty, ignore;
		private final ExternalDocs externalDocs;
		private final Items items;
		private final long maxLength, maxl, minLength, minl, maxItems, maxi, minItems, mini, maxProperties, maxp, minProperties, minp;
		private final String $ref, format, f, title, multipleOf, mo, maximum, max, minimum, min, pattern, p, type, t, collectionFormat, cf, discriminator;
		private final String[] description, d, _default, df, _enum, e, allOf, properties, additionalProperties, xml;

		Impl(Builder b) {
			super(b);
			this.$ref = b.$ref;
			this._default = copyOf(b._default);
			this._enum = copyOf(b._enum);
			this.additionalProperties = copyOf(b.additionalProperties);
			this.allOf = copyOf(b.allOf);
			this.aev = b.aev;
			this.allowEmptyValue = b.allowEmptyValue;
			this.cf = b.cf;
			this.collectionFormat = b.collectionFormat;
			this.d = copyOf(b.d);
			this.description = copyOf(b.description);
			this.df = copyOf(b.df);
			this.discriminator = b.discriminator;
			this.e = copyOf(b.e);
			this.emax = b.emax;
			this.emin = b.emin;
			this.exclusiveMaximum = b.exclusiveMaximum;
			this.exclusiveMinimum = b.exclusiveMinimum;
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
			this.sie = b.sie;
			this.skipIfEmpty = b.skipIfEmpty;
			this.t = b.t;
			this.title = b.title;
			this.type = b.type;
			this.ui = b.ui;
			this.uniqueItems = b.uniqueItems;
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
		public boolean aev() {
			return aev;
		}

		@Override /* Schema */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
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
		public boolean exclusiveMaximum() {
			return exclusiveMaximum;
		}

		@Override /* Schema */
		public boolean exclusiveMinimum() {
			return exclusiveMinimum;
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
		public boolean sie() {
			return sie;
		}

		@Override /* Schema */
		public boolean skipIfEmpty() {
			return skipIfEmpty;
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
		public String[] xml() {
			return xml;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Appliers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Applies targeted {@link Schema} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Schema,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Schema.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Schema> ai, Context.Builder b) {
			Schema a = ai.inner();
			if (isEmptyArray(a.on(), a.onClass()))
				return;
			b.annotations(a);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A collection of {@link Schema @Schema annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Schema[] value();
	}
}