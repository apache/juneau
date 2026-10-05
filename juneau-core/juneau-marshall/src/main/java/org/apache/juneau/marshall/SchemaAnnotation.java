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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.jsonschema.SchemaUtils.*;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Utility classes and methods for the {@link Schema @Schema} annotation.
 *
 */
@SuppressWarnings({
	"java:S100", // Builder methods const_/default_/else_/enum_/if_ etc. are named after JSON Schema keywords that are Java reserved words, hence the trailing underscore
	"java:S116", // Field names intentionally match JSON property names
	"java:S1192" // Duplicated literals (argument/property names) read more clearly inline than as constants
})
public class SchemaAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private SchemaAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private boolean aev;
		private boolean allowEmptyValue;
		private boolean emax;
		private boolean emin;
		private boolean ignore;
		private boolean r;
		private boolean readOnly;
		private boolean required;
		private boolean ro;
		private boolean sie;
		private boolean skipIfEmpty;
		private boolean ui;
		private boolean uniqueItems;
		private ExternalDocs externalDocs = ExternalDocsAnnotation.DEFAULT;
		private Items items = ItemsAnnotation.DEFAULT;
		private long maxi = -1;
		private long maxItems = -1;
		private long maxl = -1;
		private long maxLength = -1;
		private long maxp = -1;
		private long maxProperties = -1;
		private long mini = -1;
		private long minItems = -1;
		private long minl = -1;
		private long minLength = -1;
		private long minp = -1;
		private long minProperties = -1;
		private String $ref = "";
		private String cf = "";
		private String collectionFormat = "";
		private String discriminator = "";
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
		private String su = "";
		private String summary = "";
		private String t = "";
		private String title = "";
		private String type = "";
		private String[] default_ = {};
		private String[] enum_ = {};
		private String[] allOf = {};
		private String[] d = {};
		private String[] description = {};
		private String[] df = {};
		private String[] e = {};
		private String[] xml = {};
		private boolean deprecatedProperty;
		private String $id = "";
		private String contentMediaType = "";
		private String contentEncoding = "";
		private String exclusiveMaximumValue = "";
		private String exclusiveMinimumValue = "";
		private String[] const_ = {};
		private String[] examples = {};
		private String[] $comment = {};
		private String[] prefixItems = {};
		private String[] unevaluatedItems = {};
		private String[] unevaluatedProperties = {};
		private String[] dependentSchemas = {};
		private String[] dependentRequired = {};
		private String[] if_ = {};
		private String[] then_ = {};
		private String[] else_ = {};
		private String[] $defs = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Schema.class);
		}

		/**
		 * Sets the {@link Schema#const_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder const_(String...value) {
			const_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#default_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder default_(String...value) {
			default_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#else_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder else_(String...value) {
			else_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#enum_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder enum_(String...value) {
			enum_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#if_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder if_(String...value) {
			if_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#then_} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder then_(String...value) {
			then_ = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#$comment} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder $comment(String...value) {
			this.$comment = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#$defs} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder $defs(String...value) {
			this.$defs = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#$id} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder $id(String value) {
			this.$id = value;
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
		 * Sets the {@link Schema#aev} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder aev(boolean value) {
			aev = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#allOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allOf(String...value) {
			allOf = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#allowEmptyValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder allowEmptyValue(boolean value) {
			allowEmptyValue = value;
			return this;
		}

		/**
		 * Instantiates a new {@link Schema @Schema} object initialized with this builder.
		 *
		 * @return A new {@link Schema @Schema} object.
		 */
		public Schema build() {
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
		 * Sets the {@link Schema#cf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			cf = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#collectionFormat} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#contentEncoding} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder contentEncoding(String value) {
			contentEncoding = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#contentMediaType} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder contentMediaType(String value) {
			contentMediaType = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#d} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder d(String...value) {
			d = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#dependentRequired} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dependentRequired(String...value) {
			dependentRequired = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#dependentSchemas} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dependentSchemas(String...value) {
			dependentSchemas = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#deprecated_()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder deprecatedProperty(boolean value) {
			deprecatedProperty = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#df} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder df(String...value) {
			df = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#discriminator} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder discriminator(String value) {
			discriminator = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#e} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder e(String...value) {
			e = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#emax} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emax(boolean value) {
			emax = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#emin} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder emin(boolean value) {
			emin = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#examples} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder examples(String...value) {
			examples = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#exclusiveMaximumValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMaximumValue(String value) {
			exclusiveMaximumValue = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#exclusiveMinimumValue} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder exclusiveMinimumValue(String value) {
			exclusiveMinimumValue = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#externalDocs} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder externalDocs(ExternalDocs value) {
			externalDocs = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#f} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			f = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			format = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ignore} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ignore(boolean value) {
			ignore = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#items} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder items(Items value) {
			items = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#max} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder max(String value) {
			max = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxi} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(long value) {
			maxi = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maximum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maximum(String value) {
			maximum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxItems(long value) {
			maxItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(long value) {
			maxl = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxLength(long value) {
			maxLength = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxp} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxp(long value) {
			maxp = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#maxProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder maxProperties(long value) {
			maxProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#min} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder min(String value) {
			min = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#mini} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mini(long value) {
			mini = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minimum} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minimum(String value) {
			minimum = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minItems(long value) {
			minItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minl} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minl(long value) {
			minl = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minLength} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minLength(long value) {
			minLength = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minp} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minp(long value) {
			minp = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#minProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder minProperties(long value) {
			minProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#mo} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder mo(String value) {
			mo = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#multipleOf} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder multipleOf(String value) {
			multipleOf = value;
			return this;
		}

		// -----------------------------------------------------------------------------------------------------------------
		// JSON Schema Draft 2020-12 property setters
		// -----------------------------------------------------------------------------------------------------------------

		/**
		 * Sets the {@link Schema#p} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			p = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#pattern} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder pattern(String value) {
			pattern = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#prefixItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder prefixItems(String...value) {
			prefixItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#r} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder r(boolean value) {
			r = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#readOnly} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder readOnly(boolean value) {
			readOnly = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#required} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder required(boolean value) {
			required = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ro} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ro(boolean value) {
			ro = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#sie} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder sie(boolean value) {
			sie = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#skipIfEmpty} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder skipIfEmpty(boolean value) {
			skipIfEmpty = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#su} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder su(String value) {
			su = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#summary} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder summary(String value) {
			summary = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#t} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			t = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#title} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder title(String value) {
			title = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#type} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder type(String value) {
			type = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#ui} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder ui(boolean value) {
			ui = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#unevaluatedItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder unevaluatedItems(String...value) {
			unevaluatedItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#unevaluatedProperties} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder unevaluatedProperties(String...value) {
			unevaluatedProperties = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#uniqueItems} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uniqueItems(boolean value) {
			uniqueItems = value;
			return this;
		}

		/**
		 * Sets the {@link Schema#xml} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder xml(String...value) {
			xml = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Schema {

		private final String[] description;
		private final boolean aev;
		private final boolean allowEmptyValue;
		private final boolean emax;
		private final boolean emin;
		private final boolean uniqueItems;
		private final boolean ui;
		private final boolean required;
		private final boolean r;
		private final boolean readOnly;
		private final boolean ro;
		private final boolean sie;
		private final boolean skipIfEmpty;
		private final boolean ignore;
		private final ExternalDocs externalDocs;
		private final Items items;
		private final long maxLength;
		private final long maxl;
		private final long minLength;
		private final long minl;
		private final long maxItems;
		private final long maxi;
		private final long minItems;
		private final long mini;
		private final long maxProperties;
		private final long maxp;
		private final long minProperties;
		private final long minp;
		private final String $ref;
		private final String format;
		private final String f;
		private final String title;
		private final String multipleOf;
		private final String mo;
		private final String maximum;
		private final String max;
		private final String minimum;
		private final String min;
		private final String pattern;
		private final String p;
		private final String su;
		private final String summary;
		private final String type;
		private final String t;
		private final String collectionFormat;
		private final String cf;
		private final String discriminator;
		private final String[] d;
		private final String[] default_;
		private final String[] df;
		private final String[] enum_;
		private final String[] e;
		private final String[] allOf;
		private final String[] xml;
		// JSON Schema Draft 2020-12 fields
		private final boolean deprecatedProperty;
		private final String $id;
		private final String contentMediaType;
		private final String contentEncoding;
		private final String exclusiveMaximumValue;
		private final String exclusiveMinimumValue;
		private final String[] const_;
		private final String[] examples;
		private final String[] $comment;
		private final String[] prefixItems;
		private final String[] unevaluatedItems;
		private final String[] unevaluatedProperties;
		private final String[] dependentSchemas;
		private final String[] dependentRequired;
		private final String[] if_;
		private final String[] then_;
		private final String[] else_;
		private final String[] $defs;

		Object(SchemaAnnotation.Builder b) {
			super(b);
			description = cp(b.description);
			$ref = b.$ref;
			default_ = cp(b.default_);
			enum_ = cp(b.enum_);
			allOf = cp(b.allOf);
			aev = b.aev;
			allowEmptyValue = b.allowEmptyValue;
			cf = b.cf;
			collectionFormat = b.collectionFormat;
			d = cp(b.d);
			df = cp(b.df);
			discriminator = b.discriminator;
			e = cp(b.e);
			emax = b.emax;
			emin = b.emin;
			externalDocs = b.externalDocs;
			f = b.f;
			format = b.format;
			ignore = b.ignore;
			items = b.items;
			max = b.max;
			maxi = b.maxi;
			maximum = b.maximum;
			maxItems = b.maxItems;
			maxl = b.maxl;
			maxLength = b.maxLength;
			maxp = b.maxp;
			maxProperties = b.maxProperties;
			min = b.min;
			mini = b.mini;
			minimum = b.minimum;
			minItems = b.minItems;
			minl = b.minl;
			minLength = b.minLength;
			minp = b.minp;
			minProperties = b.minProperties;
			mo = b.mo;
			multipleOf = b.multipleOf;
			p = b.p;
			pattern = b.pattern;
			r = b.r;
			readOnly = b.readOnly;
			required = b.required;
			ro = b.ro;
			sie = b.sie;
			skipIfEmpty = b.skipIfEmpty;
			su = b.su;
			summary = b.summary;
			t = b.t;
			title = b.title;
			type = b.type;
			ui = b.ui;
			uniqueItems = b.uniqueItems;
			xml = cp(b.xml);
			deprecatedProperty = b.deprecatedProperty;
			$id = b.$id;
			contentMediaType = b.contentMediaType;
			contentEncoding = b.contentEncoding;
			exclusiveMaximumValue = b.exclusiveMaximumValue;
			exclusiveMinimumValue = b.exclusiveMinimumValue;
			const_ = cp(b.const_);
			examples = cp(b.examples);
			$comment = cp(b.$comment);
			prefixItems = cp(b.prefixItems);
			unevaluatedItems = cp(b.unevaluatedItems);
			unevaluatedProperties = cp(b.unevaluatedProperties);
			dependentSchemas = cp(b.dependentSchemas);
			dependentRequired = cp(b.dependentRequired);
			if_ = cp(b.if_);
			then_ = cp(b.then_);
			else_ = cp(b.else_);
			$defs = cp(b.$defs);
		}

		@Override /* Overridden from Schema */
		public String[] const_() {
			return const_;
		}

		@Override /* Overridden from Schema */
		public String[] default_() {
			return default_;
		}

		@Override /* Overridden from Schema */
		public String[] else_() {
			return else_;
		}

		@Override /* Overridden from Schema */
		public String[] enum_() {
			return enum_;
		}

		@Override /* Overridden from Schema */
		public String[] if_() {
			return if_;
		}

		@Override /* Overridden from Schema */
		public String[] then_() {
			return then_;
		}

		@Override /* Overridden from Schema */
		public String[] $comment() {
			return $comment;
		}

		@Override /* Overridden from Schema */
		public String[] $defs() {
			return $defs;
		}

		@Override /* Overridden from Schema */
		public String $id() {
			return $id;
		}

		@Override /* Overridden from Schema */
		public String $ref() {
			return $ref;
		}

		@Override /* Overridden from Schema */
		public boolean aev() {
			return aev;
		}

		@Override /* Overridden from Schema */
		public String[] allOf() {
			return allOf;
		}

		@Override /* Overridden from Schema */
		public boolean allowEmptyValue() {
			return allowEmptyValue;
		}

		@Override /* Overridden from Schema */
		public String cf() {
			return cf;
		}

		@Override /* Overridden from Schema */
		public String collectionFormat() {
			return collectionFormat;
		}

		@Override /* Overridden from Schema */
		public String contentEncoding() {
			return contentEncoding;
		}

		@Override /* Overridden from Schema */
		public String contentMediaType() {
			return contentMediaType;
		}

		@Override /* Overridden from Schema */
		public String[] d() {
			return d;
		}

		@Override /* Overridden from Schema */
		public String[] dependentRequired() {
			return dependentRequired;
		}

		@Override /* Overridden from Schema */
		public String[] dependentSchemas() {
			return dependentSchemas;
		}

		@Override /* Overridden from Schema */
		public boolean deprecated_() {
			return deprecatedProperty;
		}

		@Override /* Overridden from Schema */
		public String[] df() {
			return df;
		}

		@Override /* Overridden from Schema */
		public String discriminator() {
			return discriminator;
		}

		@Override /* Overridden from Schema */
		public String[] e() {
			return e;
		}

		@Override /* Overridden from Schema */
		public boolean emax() {
			return emax;
		}

		@Override /* Overridden from Schema */
		public boolean emin() {
			return emin;
		}

		@Override /* Overridden from Schema */
		public String[] examples() {
			return examples;
		}

		@Override /* Overridden from Schema */
		public String exclusiveMaximumValue() {
			return exclusiveMaximumValue;
		}

		@Override /* Overridden from Schema */
		public String exclusiveMinimumValue() {
			return exclusiveMinimumValue;
		}

		@Override /* Overridden from Schema */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* Overridden from Schema */
		public String f() {
			return f;
		}

		@Override /* Overridden from Schema */
		public String format() {
			return format;
		}

		@Override /* Overridden from Schema */
		public boolean ignore() {
			return ignore;
		}

		@Override /* Overridden from Schema */
		public Items items() {
			return items;
		}

		@Override /* Overridden from Schema */
		public String max() {
			return max;
		}

		@Override /* Overridden from Schema */
		public long maxi() {
			return maxi;
		}

		@Override /* Overridden from Schema */
		public String maximum() {
			return maximum;
		}

		@Override /* Overridden from Schema */
		public long maxItems() {
			return maxItems;
		}

		@Override /* Overridden from Schema */
		public long maxl() {
			return maxl;
		}

		@Override /* Overridden from Schema */
		public long maxLength() {
			return maxLength;
		}

		@Override /* Overridden from Schema */
		public long maxp() {
			return maxp;
		}

		@Override /* Overridden from Schema */
		public long maxProperties() {
			return maxProperties;
		}

		@Override /* Overridden from Schema */
		public String min() {
			return min;
		}

		@Override /* Overridden from Schema */
		public long mini() {
			return mini;
		}

		@Override /* Overridden from Schema */
		public String minimum() {
			return minimum;
		}

		@Override /* Overridden from Schema */
		public long minItems() {
			return minItems;
		}

		@Override /* Overridden from Schema */
		public long minl() {
			return minl;
		}

		@Override /* Overridden from Schema */
		public long minLength() {
			return minLength;
		}

		@Override /* Overridden from Schema */
		public long minp() {
			return minp;
		}

		@Override /* Overridden from Schema */
		public long minProperties() {
			return minProperties;
		}

		@Override /* Overridden from Schema */
		public String mo() {
			return mo;
		}

		@Override /* Overridden from Schema */
		public String multipleOf() {
			return multipleOf;
		}

		// -----------------------------------------------------------------------------------------------------------------
		// JSON Schema Draft 2020-12 property getters
		// -----------------------------------------------------------------------------------------------------------------

		@Override /* Overridden from Schema */
		public String p() {
			return p;
		}

		@Override /* Overridden from Schema */
		public String pattern() {
			return pattern;
		}

		@Override /* Overridden from Schema */
		public String[] prefixItems() {
			return prefixItems;
		}

		@Override /* Overridden from Schema */
		public boolean r() {
			return r;
		}

		@Override /* Overridden from Schema */
		public boolean readOnly() {
			return readOnly;
		}

		@Override /* Overridden from Schema */
		public boolean required() {
			return required;
		}

		@Override /* Overridden from Schema */
		public boolean ro() {
			return ro;
		}

		@Override /* Overridden from Schema */
		public boolean sie() {
			return sie;
		}

		@Override /* Overridden from Schema */
		public boolean skipIfEmpty() {
			return skipIfEmpty;
		}

		@Override /* Overridden from Schema */
		public String su() {
			return su;
		}

		@Override /* Overridden from Schema */
		public String summary() {
			return summary;
		}

		@Override /* Overridden from Schema */
		public String t() {
			return t;
		}

		@Override /* Overridden from Schema */
		public String title() {
			return title;
		}

		@Override /* Overridden from Schema */
		public String type() {
			return type;
		}

		@Override /* Overridden from Schema */
		public boolean ui() {
			return ui;
		}

		@Override /* Overridden from Schema */
		public String[] unevaluatedItems() {
			return unevaluatedItems;
		}

		@Override /* Overridden from Schema */
		public String[] unevaluatedProperties() {
			return unevaluatedProperties;
		}

		@Override /* Overridden from Schema */
		public boolean uniqueItems() {
			return uniqueItems;
		}

		@Override /* Overridden from Schema */
		public String[] xml() {
			return xml;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Schema DEFAULT = create().build();

	/**
	 * Converts the specified <ja>@Schema</ja> annotation into a generic map.
	 *
	 * @param a The annotation instance.  Can be <jk>null</jk> (returns an empty map).
	 * @return The schema converted to a map, or an empty map if the annotation was null.
	 * @throws ParseException Malformed input encountered.
	 */
	public static JsonMap asMap(Schema a) throws ParseException {
		if (a == null)
			return JsonMap.EMPTY_MAP;
		var m = new JsonMap();
		if (SchemaAnnotation.empty(a))
			return m;
		Predicate<String> ne = Shorts::ine;
		Predicate<Collection<?>> nec = Shorts::ine;
		Predicate<Map<?,?>> nem = Shorts::ine;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = Shorts::nm1;

		// Handle exclusiveMaximum with Draft 2020-12 fallback
		String exclusiveMaximumValue;
		if (ne.test(a.exclusiveMaximumValue())) {
			exclusiveMaximumValue = a.exclusiveMaximumValue();
		} else if (a.emax()) {
			exclusiveMaximumValue = "true";
		} else {
			exclusiveMaximumValue = null;
		}

		// Handle exclusiveMinimum with Draft 2020-12 fallback
		String exclusiveMinimumValue;
		if (ne.test(a.exclusiveMinimumValue())) {
			exclusiveMinimumValue = a.exclusiveMinimumValue();
		} else if (a.emin()) {
			exclusiveMinimumValue = "true";
		} else {
			exclusiveMinimumValue = null;
		}

		// @formatter:off
		return m
			.appendIf(ne, "allOf", joinnl(a.allOf()))
			.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf())
			.appendIf(ne, "default", joinnl(a.default_(), a.df()))
			.appendIf(ne, "discriminator", a.discriminator())
			.appendIf(ne, "description", joinnl(a.description(), a.d()))
			.appendFirst(nec, "enum", parseSet(a.enum_()), parseSet(a.e()))
			.appendIf(ne, "exclusiveMaximum", exclusiveMaximumValue)
			.appendIf(ne, "exclusiveMinimum", exclusiveMinimumValue)
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
			.appendIf(nf, "readOnly", a.readOnly() || a.ro())
			.appendIf(nf, "required", a.required() || a.r())
			.appendFirst(ne, "summary", a.summary(), a.su())
			.appendIf(ne, "title", a.title())
			.appendFirst(ne, "type", a.type(), a.t())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui())
			.appendIf(ne, "xml", joinnl(a.xml()))
			.appendIf(ne, "$ref", a.$ref())
			// JSON Schema Draft 2020-12 properties
			.appendIf(ne, "const", joinnl(a.const_()))
			.appendIf(nec, "examples", a.examples().length == 0 ? null : l(a.examples()))
			.appendIf(ne, "$comment", joinnl(a.$comment()))
			.appendIf(nf, "deprecated", a.deprecated_())
			.appendIf(ne, "contentMediaType", a.contentMediaType())
			.appendIf(ne, "contentEncoding", a.contentEncoding())
			.appendIf(ne, "prefixItems", joinnl(a.prefixItems()))
			.appendIf(ne, "unevaluatedItems", joinnl(a.unevaluatedItems()))
			.appendIf(ne, "unevaluatedProperties", joinnl(a.unevaluatedProperties()))
			.appendIf(ne, "dependentSchemas", joinnl(a.dependentSchemas()))
			.appendIf(ne, "dependentRequired", joinnl(a.dependentRequired()))
			.appendIf(ne, "if", joinnl(a.if_()))
			.appendIf(ne, "then", joinnl(a.then_()))
			.appendIf(ne, "else", joinnl(a.else_()))
			.appendIf(ne, "$defs", joinnl(a.$defs()))
			.appendIf(ne, "$id", a.$id())
		;
		// @formatter:on
	}

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
	 * @param a The annotation to check. Can be <jk>null</jk> (returns <jk>true</jk>).
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Schema a) {
		return a == null || DEFAULT.equals(a);
	}

	private static JsonMap merge(JsonMap m, Items a) throws ParseException {
		if (ItemsAnnotation.empty(a))
			return m;
		if (m == null)
			m = new JsonMap();
		Predicate<String> ne = Shorts::ine;
		Predicate<Collection<?>> nec = Shorts::ine;
		Predicate<Map<?,?>> nem = Shorts::ine;
		Predicate<Boolean> nf = ObjectUtils::isTrue;
		Predicate<Long> nm1 = Shorts::nm1;
		return m.appendFirst(ne, "collectionFormat", a.collectionFormat(), a.cf()).appendIf(ne, "default", joinnl(a.default_(), a.df())).appendFirst(nec, "enum", parseSet(a.enum_()), parseSet(a.e()))
			.appendFirst(ne, "format", a.format(), a.f()).appendIf(nf, "exclusiveMaximum", a.exclusiveMaximum() || a.emax()).appendIf(nf, "exclusiveMinimum", a.exclusiveMinimum() || a.emin())
			.appendIf(nem, "items", SubItemsAnnotation.merge(m.getMap("items"), a.items())).appendFirst(ne, "maximum", a.maximum(), a.max()).appendFirst(nm1, "maxItems", a.maxItems(), a.maxi())
			.appendFirst(nm1, "maxLength", a.maxLength(), a.maxl()).appendFirst(ne, "minimum", a.minimum(), a.min()).appendFirst(nm1, "minItems", a.minItems(), a.mini())
			.appendFirst(nm1, "minLength", a.minLength(), a.minl()).appendFirst(ne, "multipleOf", a.multipleOf(), a.mo()).appendFirst(ne, "pattern", a.pattern(), a.p())
			.appendIf(nf, "uniqueItems", a.uniqueItems() || a.ui()).appendFirst(ne, "type", a.type(), a.t()).appendIf(ne, "$ref", a.$ref());
	}
}