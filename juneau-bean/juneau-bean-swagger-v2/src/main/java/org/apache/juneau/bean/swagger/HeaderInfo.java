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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshaller.*;

/**
 * Describes a single HTTP header.
 *
 * <p>
 * The Header Object follows the structure of the Parameter Object with the following changes: it does not have a
 * <c>name</c> field since the header name is specified in the key, and it does not have a <c>required</c> field
 * since headers are always optional in HTTP for Swagger 2.0.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Header Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>description</c> (string) - A brief description of the header
 * 	<li><c>type</c> (string, REQUIRED) - The type of the header. Values: <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js>
 * 	<li><c>format</c> (string) - The format modifier (e.g., <js>"int32"</js>, <js>"int64"</js>, <js>"float"</js>, <js>"double"</js>, <js>"date"</js>, <js>"date-time"</js>)
 * 	<li><c>items</c> ({@link Items}) - Required if type is <js>"array"</js>. Describes the type of items in the array
 * 	<li><c>collectionFormat</c> (string) - How multiple values are formatted. Values: <js>"csv"</js>, <js>"ssv"</js>, <js>"tsv"</js>, <js>"pipes"</js>, <js>"multi"</js>
 * 	<li><c>default</c> (any) - The default value
 * 	<li><c>maximum</c> (number), <c>exclusiveMaximum</c> (boolean), <c>minimum</c> (number), <c>exclusiveMinimum</c> (boolean) - Numeric constraints
 * 	<li><c>maxLength</c> (integer), <c>minLength</c> (integer), <c>pattern</c> (string) - String constraints
 * 	<li><c>maxItems</c> (integer), <c>minItems</c> (integer), <c>uniqueItems</c> (boolean) - Array constraints
 * 	<li><c>enum</c> (array) - Possible values for this header
 * 	<li><c>multipleOf</c> (number) - Must be a multiple of this value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo <jv>headerInfo</jv> = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>headerInfo</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>headerInfo</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#header-object">Swagger 2.0 Specification &gt; Header Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/describing-responses/">Swagger Describing Responses</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
@SuppressWarnings({"java:S115", "java:S116"})
public class HeaderInfo extends SwaggerElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_property = "property";

	// Property name constants
	private static final String PROP_collectionFormat = "collectionFormat";
	private static final String PROP_default = "default";
	private static final String PROP_description = "description";
	private static final String PROP_example = "example";
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
	private static final String PROP_enum = "enum";
	private static final String PROP_type = "type";
	private static final String PROP_$ref = "$ref";
	private static final String PROP_uniqueItems = "uniqueItems";

	private static final String[] VALID_TYPES = { "string", "number", "integer", "boolean", "array" };
	private static final String[] VALID_COLLECTION_FORMATS = { "csv", "ssv", "tsv", "pipes", "multi" };

	private String description;
	private String type;
	private String format;
	private String collectionFormat;
	private String pattern;
	private String ref;
	private Number maximum;
	private Number minimum;
	private Number multipleOf;
	private Integer maxLength;
	private Integer minLength;
	private Integer maxItems;
	private Integer minItems;
	private Boolean exclusiveMaximum;
	private Boolean exclusiveMinimum;
	private Boolean uniqueItems;
	private Items items;
	private Object default_;
	private Set<Object> enum_ = new LinkedHashSet<>();
	private Object example;

	/**
	 * Default constructor.
	 */
	public HeaderInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public HeaderInfo(HeaderInfo copyFrom) {
		super(copyFrom);

		this.collectionFormat = copyFrom.collectionFormat;
		this.default_ = copyFrom.default_;
		this.description = copyFrom.description;
		if (nn(copyFrom.enum_))
			this.enum_.addAll(copyOf(copyFrom.enum_));
		this.example = copyFrom.example;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.format = copyFrom.format;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.maximum = copyFrom.maximum;
		this.maxItems = copyFrom.maxItems;
		this.maxLength = copyFrom.maxLength;
		this.minimum = copyFrom.minimum;
		this.minItems = copyFrom.minItems;
		this.minLength = copyFrom.minLength;
		this.multipleOf = copyFrom.multipleOf;
		this.pattern = copyFrom.pattern;
		this.ref = copyFrom.ref;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public HeaderInfo addEnum(Object...value) {
		if (nn(value))
			for (var v : value)
				if (nn(v))
					enum_.add(v);
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public HeaderInfo copy() {
		return new HeaderInfo(this);
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_collectionFormat -> toType(getCollectionFormat(), type);
			case PROP_default -> toType(getDefault(), type);
			case PROP_description -> toType(getDescription(), type);
			case PROP_enum -> toType(getEnum(), type);
			case PROP_example -> toType(getExample(), type);
			case PROP_exclusiveMaximum -> toType(getExclusiveMaximum(), type);
			case PROP_exclusiveMinimum -> toType(getExclusiveMinimum(), type);
			case PROP_format -> toType(getFormat(), type);
			case PROP_items -> toType(getItems(), type);
			case PROP_maximum -> toType(getMaximum(), type);
			case PROP_maxItems -> toType(getMaxItems(), type);
			case PROP_maxLength -> toType(getMaxLength(), type);
			case PROP_minimum -> toType(getMinimum(), type);
			case PROP_minItems -> toType(getMinItems(), type);
			case PROP_minLength -> toType(getMinLength(), type);
			case PROP_multipleOf -> toType(getMultipleOf(), type);
			case PROP_pattern -> toType(getPattern(), type);
			case PROP_$ref -> toType(getRef(), type);
			case PROP_type -> toType(getType(), type);
			case PROP_uniqueItems -> toType(getUniqueItems(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() { return collectionFormat; }

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() { return default_; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getEnum() { return nullIfEmpty(enum_); }

	/**
	 * Bean property getter:  <property>example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getExample() { return example; }

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() { return exclusiveMaximum; }

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() { return exclusiveMinimum; }

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() { return format; }

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() { return items; }

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() { return maximum; }

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() { return maxItems; }

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() { return maxLength; }

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() { return minimum; }

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() { return minItems; }

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() { return minLength; }

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() { return multipleOf; }

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() { return pattern; }

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() { return ref; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() { return uniqueItems; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(ref), PROP_$ref)
			.addIf(nn(collectionFormat), PROP_collectionFormat)
			.addIf(nn(default_), PROP_default)
			.addIf(nn(description), PROP_description)
			.addIf(ne(enum_), PROP_enum)
			.addIf(nn(example), PROP_example)
			.addIf(nn(exclusiveMaximum), PROP_exclusiveMaximum)
			.addIf(nn(exclusiveMinimum), PROP_exclusiveMinimum)
			.addIf(nn(format), PROP_format)
			.addIf(nn(items), PROP_items)
			.addIf(nn(maxItems), PROP_maxItems)
			.addIf(nn(maxLength), PROP_maxLength)
			.addIf(nn(maximum), PROP_maximum)
			.addIf(nn(minItems), PROP_minItems)
			.addIf(nn(minLength), PROP_minLength)
			.addIf(nn(minimum), PROP_minimum)
			.addIf(nn(multipleOf), PROP_multipleOf)
			.addIf(nn(pattern), PROP_pattern)
			.addIf(nn(type), PROP_type)
			.addIf(nn(uniqueItems), PROP_uniqueItems)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param swagger The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <c>$ref</c> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public HeaderInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (nn(ref)) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = swagger.findRef(ref, HeaderInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (nn(items))
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public HeaderInfo set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_collectionFormat -> setCollectionFormat(s(value));
			case PROP_default -> setDefault(value);
			case PROP_description -> setDescription(s(value));
			case PROP_enum -> setEnum(setb(Object.class).sparse().addAny(value).build());
			case PROP_example -> setExample(value);
			case PROP_exclusiveMaximum -> setExclusiveMaximum(toBoolean(value));
			case PROP_exclusiveMinimum -> setExclusiveMinimum(toBoolean(value));
			case PROP_format -> setFormat(s(value));
			case PROP_items -> setItems(toType(value, Items.class));
			case PROP_maximum -> setMaximum(toNumber(value));
			case PROP_maxItems -> setMaxItems(toInteger(value));
			case PROP_maxLength -> setMaxLength(toInteger(value));
			case PROP_minimum -> setMinimum(toNumber(value));
			case PROP_minItems -> setMinItems(toInteger(value));
			case PROP_minLength -> setMinLength(toInteger(value));
			case PROP_multipleOf -> setMultipleOf(toNumber(value));
			case PROP_pattern -> setPattern(s(value));
			case PROP_$ref -> setRef(s(value));
			case PROP_type -> setType(s(value));
			case PROP_uniqueItems -> setUniqueItems(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"csv"</js> (default) - comma separated values <c>foo,bar</c>.
	 * 		<li><js>"ssv"</js> - space separated values <c>foo bar</c>.
	 * 		<li><js>"tsv"</js> - tab separated values <c>foo\tbar</c>.
	 * 		<li><js>"pipes"</js> - pipe separated values <c>foo|bar</c>.
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw rex("Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_COLLECTION_FORMATS));
		collectionFormat = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setDefault(Object value) {
		default_ = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setEnum(Collection<Object> value) {
		enum_.clear();
		if (nn(value))
			enum_.addAll(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setEnum(Object...value) {
		return setEnum(l(value));
	}

	/**
	 * Bean property setter:  <property>example</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setFormat(String value) {
		format = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required if <c>type</c> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setItems(Items value) {
		items = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaximum(Number value) {
		maximum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaxItems(Integer value) {
		maxItems = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaxLength(Integer value) {
		maxLength = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinimum(Number value) {
		minimum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinItems(Integer value) {
		minItems = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinLength(Integer value) {
		minLength = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMultipleOf(Number value) {
		multipleOf = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This string SHOULD be a valid regular expression.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setPattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@Beanp("$ref")
	public HeaderInfo setRef(String value) {
		ref = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * @return This object.
	 */
	public HeaderInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw rex("Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_TYPES));
		type = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public HeaderInfo strict() {
		super.strict();
		return this;
	}
}