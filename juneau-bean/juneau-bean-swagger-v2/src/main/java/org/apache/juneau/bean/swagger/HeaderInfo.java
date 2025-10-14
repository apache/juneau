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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
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
public class HeaderInfo extends SwaggerElement {

	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv","ssv","tsv","pipes","multi"};

	private String
		description,
		type,
		format,
		collectionFormat,
		pattern,
		ref;
	private Number
		maximum,
		minimum,
		multipleOf;
	private Integer
		maxLength,
		minLength,
		maxItems,
		minItems;
	private Boolean
		exclusiveMaximum,
		exclusiveMinimum,
		uniqueItems;
	private Items items;
	private Object _default;  // NOSONAR - Intentional naming.
	private Set<Object> _enum;  // NOSONAR - Intentional naming.
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
		this._default = copyFrom._default;
		this.description = copyFrom.description;
		this._enum = copyOf(copyFrom._enum);
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
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public HeaderInfo copy() {
		return new HeaderInfo(this);
	}

	@Override /* Overridden from SwaggerElement */
	public HeaderInfo strict() {
		super.strict();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() {
		return collectionFormat;
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
		if (isStrict() && ! ArrayUtils.contains(value, VALID_COLLECTION_FORMATS))
			throw new BasicRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_COLLECTION_FORMATS)
			);
		collectionFormat = value;
		return this;
	}

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
	public Object getDefault() {
		return _default;
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
		_default = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
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
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getEnum() {
		return _enum;
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
		_enum = setFrom(value);
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
		return setEnum(Arrays.asList(value));
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
		setEnum(setBuilder(_enum).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getExample() {
		return example;
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
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
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
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
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
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
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
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
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
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
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
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
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
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
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
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
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
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
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
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
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
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
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
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
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
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() {
		return ref;
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
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
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
		if (isStrict() && ! ArrayUtils.contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_TYPES)
			);
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
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
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "collectionFormat" -> toType(getCollectionFormat(), type);
			case "default" -> toType(getDefault(), type);
			case "description" -> (T)getDescription();
			case "enum" -> toType(getEnum(), type);
			case "example" -> toType(getExample(), type);
			case "exclusiveMaximum" -> toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum" -> toType(getExclusiveMinimum(), type);
			case "format" -> toType(getFormat(), type);
			case "items" -> toType(getItems(), type);
			case "maximum" -> toType(getMaximum(), type);
			case "maxItems" -> toType(getMaxItems(), type);
			case "maxLength" -> toType(getMaxLength(), type);
			case "minimum" -> toType(getMinimum(), type);
			case "minItems" -> toType(getMinItems(), type);
			case "minLength" -> toType(getMinLength(), type);
			case "multipleOf" -> toType(getMultipleOf(), type);
			case "pattern" -> toType(getPattern(), type);
			case "$ref" -> toType(getRef(), type);
			case "type" -> toType(getType(), type);
			case "uniqueItems" -> toType(getUniqueItems(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from SwaggerElement */
	public HeaderInfo set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "collectionFormat" -> setCollectionFormat(Utils.s(value));
			case "default" -> setDefault(value);
			case "description" -> setDescription(Utils.s(value));
			case "enum" -> setEnum(setBuilder(Object.class).sparse().addAny(value).build());
			case "example" -> setExample(value);
			case "exclusiveMaximum" -> setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum" -> setExclusiveMinimum(toBoolean(value));
			case "format" -> setFormat(Utils.s(value));
			case "items" -> setItems(toType(value, Items.class));
			case "maximum" -> setMaximum(toNumber(value));
			case "maxItems" -> setMaxItems(toInteger(value));
			case "maxLength" -> setMaxLength(toInteger(value));
			case "minimum" -> setMinimum(toNumber(value));
			case "minItems" -> setMinItems(toInteger(value));
			case "minLength" -> setMinLength(toInteger(value));
			case "multipleOf" -> setMultipleOf(toNumber(value));
			case "pattern" -> setPattern(Utils.s(value));
			case "$ref" -> setRef(Utils.s(value));
			case "type" -> setType(Utils.s(value));
			case "uniqueItems" -> setUniqueItems(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(collectionFormat != null, "collectionFormat")
			.addIf(_default != null, "default")
			.addIf(description != null, "description")
			.addIf(_enum != null, "enum")
			.addIf(example != null, "example")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(format != null, "format")
			.addIf(items != null, "items")
			.addIf(maximum != null, "maximum")
			.addIf(maxItems != null, "maxItems")
			.addIf(maxLength != null, "maxLength")
			.addIf(minimum != null, "minimum")
			.addIf(minItems != null, "minItems")
			.addIf(minLength != null, "minLength")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(pattern != null, "pattern")
			.addIf(ref != null, "$ref")
			.addIf(type != null, "type")
			.addIf(uniqueItems != null, "uniqueItems")
			.build();
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

		if (ref != null) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = swagger.findRef(ref, HeaderInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}
}