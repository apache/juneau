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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.CollectionUtils.copyOf;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * A limited subset of JSON-Schema's items object.
 *
 * <p>
 * The Items Object is a limited subset of JSON-Schema's items object. It is used by parameter definitions that are 
 * not located in "body" to describe the type of items in an array. This is particularly useful for query parameters, 
 * path parameters, and header parameters that accept arrays.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Items Object supports the following fields from JSON Schema:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string, REQUIRED) - The data type. Values: <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js>
 * 	<li><c>format</c> (string) - The format modifier (e.g., <js>"int32"</js>, <js>"int64"</js>, <js>"float"</js>, <js>"double"</js>, <js>"date"</js>, <js>"date-time"</js>)
 * 	<li><c>items</c> ({@link Items}) - Required if type is <js>"array"</js>. Describes the type of items in the array
 * 	<li><c>collectionFormat</c> (string) - How multiple values are formatted. Values: <js>"csv"</js>, <js>"ssv"</js>, <js>"tsv"</js>, <js>"pipes"</js>, <js>"multi"</js>
 * 	<li><c>default</c> (any) - The default value
 * 	<li><c>maximum</c> (number), <c>exclusiveMaximum</c> (boolean), <c>minimum</c> (number), <c>exclusiveMinimum</c> (boolean) - Numeric constraints
 * 	<li><c>maxLength</c> (integer), <c>minLength</c> (integer), <c>pattern</c> (string) - String constraints
 * 	<li><c>maxItems</c> (integer), <c>minItems</c> (integer), <c>uniqueItems</c> (boolean) - Array constraints
 * 	<li><c>enum</c> (array) - Possible values for this item
 * 	<li><c>multipleOf</c> (number) - Must be a multiple of this value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Items <jv>x</jv> = <jsm>items</jsm>(<js>"string"</js>).minLength(2);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String <jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"type"</js>: <js>"string"</js>,
 * 		<js>"minLength"</js>: 2
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#items-object">OpenAPI Specification &gt; Items Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-parameters/">OpenAPI Describing Parameters</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@FluentSetters
public class Items extends OpenApiElement {

	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv","ssv","tsv","pipes","multi"};

	private String
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
	private Items items;  // NOSONAR - Intentional naming.
	private Object _default;  // NOSONAR - Intentional naming.
	private List<Object> _enum;  // NOSONAR - Intentional naming.

	/**
	 * Default constructor.
	 */
	public Items() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Items(Items copyFrom) {
		super(copyFrom);

		this.type = copyFrom.type;
		this.format = copyFrom.format;
		this.collectionFormat = copyFrom.collectionFormat;
		this.pattern = copyFrom.pattern;
		this.maximum = copyFrom.maximum;
		this.minimum = copyFrom.minimum;
		this.multipleOf = copyFrom.multipleOf;
		this.maxLength = copyFrom.maxLength;
		this.minLength = copyFrom.minLength;
		this.maxItems = copyFrom.maxItems;
		this.minItems = copyFrom.minItems;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.uniqueItems = copyFrom.uniqueItems;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this._default = copyFrom._default;
		this._enum = copyOf(copyFrom._enum);
		this.ref = copyFrom.ref;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Items copy() {
		return new Items(this);
	}


	@Override /* SwaggerElement */
	protected Items strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - do not modify */
	public Items strict(Object value) {
		super.strict(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The internal type of the array.
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
	 * The internal type of the array.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new IllegalArgumentException(
				"Invalid value passed in to setType(String).  Value='"+value+"', valid values="
				+ Json5Serializer.DEFAULT.toString(VALID_TYPES));
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <code>type</code>.
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
	 * The extending format for the previously mentioned <code>type</code>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setFormat(String value) {
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
	 * 	<br>Property value is required if <code>type</code> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setItems(Items value) {
		items = value;
		return this;
	}

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
	 * 		<li><js>"csv"</js> (default) - comma separated values <code>foo,bar</code>.
	 * 		<li><js>"ssv"</js> - space separated values <code>foo bar</code>.
	 * 		<li><js>"tsv"</js> - tab separated values <code>foo\tbar</code>.
	 * 		<li><js>"pipes"</js> - pipe separated values <code>foo|bar</code>.
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw new BasicRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, VALID_COLLECTION_FORMATS
			);
		collectionFormat = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
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
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setDefault(Object value) {
		_default = value;
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
	 * @return This object
	 */
	public Items setMaximum(Number value) {
		maximum = value;
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
	 * @return This object
	 */
	public Items setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
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
	 * @return This object
	 */
	public Items setMinimum(Number value) {
		minimum = value;
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
	 * @return This object
	 */
	public Items setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
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
	 * @return This object
	 */
	public Items setMaxLength(Integer value) {
		maxLength = value;
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
	 * @return This object
	 */
	public Items setMinLength(Integer value) {
		minLength = value;
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
	 * <p>
	 * This string SHOULD be a valid regular expression.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setPattern(String value) {
		pattern = value;
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
	 * @return This object
	 */
	public Items setMaxItems(Integer value) {
		maxItems = value;
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
	 * @return This object
	 */
	public Items setMinItems(Integer value) {
		minItems = value;
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
	 * @return This object
	 */
	public Items setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Items setEnum(Collection<Object> value) {
		_enum = listFrom(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Items addEnum(Object...values) {
		_enum = listBuilder(_enum).elementType(Object.class).sparse().addAny(values).build();
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Object</code>
	 * 		<li><code>Collection&lt;Object&gt;</code>
	 * 		<li><code>String</code> - JSON array representation of <code>Collection&lt;Object&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	_enum(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	_enum(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public Items setEnum(Object...values) {  // NOSONAR - Intentional naming.
		_enum = listBuilder(_enum).elementType(Object.class).sparse().addAny(values).build();
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
	 * @return This object
	 */
	public Items setMultipleOf(Number value) {
		multipleOf = value;
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
	 * @return This object
	 */
	@Beanp("$ref")
	public Items setRef(Object value) {
		ref = Utils.s(value);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "type" -> toType(getType(), type);
			case "format" -> toType(getFormat(), type);
			case "items" -> toType(getItems(), type);
			case "collectionFormat" -> toType(getCollectionFormat(), type);
			case "default" -> toType(getDefault(), type);
			case "maximum" -> toType(getMaximum(), type);
			case "exclusiveMaximum" -> toType(getExclusiveMaximum(), type);
			case "minimum" -> toType(getMinimum(), type);
			case "exclusiveMinimum" -> toType(getExclusiveMinimum(), type);
			case "maxLength" -> toType(getMaxLength(), type);
			case "minLength" -> toType(getMinLength(), type);
			case "pattern" -> toType(getPattern(), type);
			case "maxItems" -> toType(getMaxItems(), type);
			case "minItems" -> toType(getMinItems(), type);
			case "uniqueItems" -> toType(getUniqueItems(), type);
			case "enum" -> toType(getEnum(), type);
			case "multipleOf" -> toType(getMultipleOf(), type);
			case "$ref" -> toType(getRef(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* SwaggerElement */
	public Items set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "$ref" -> setRef(value);
			case "collectionFormat" -> setCollectionFormat(Utils.s(value));
			case "default" -> setDefault(value);
			case "enum" -> setEnum(value);
			case "exclusiveMaximum" -> setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum" -> setExclusiveMinimum(toBoolean(value));
			case "format" -> setFormat(Utils.s(value));
			case "items" -> setItems(toType(value, Items.class));
			case "maxItems" -> setMaxItems(toInteger(value));
			case "maxLength" -> setMaxLength(toInteger(value));
			case "maximum" -> setMaximum(toNumber(value));
			case "minItems" -> setMinItems(toInteger(value));
			case "minLength" -> setMinLength(toInteger(value));
			case "minimum" -> setMinimum(toNumber(value));
			case "multipleOf" -> setMultipleOf(toNumber(value));
			case "pattern" -> setPattern(Utils.s(value));
			case "type" -> setType(Utils.s(value));
			case "uniqueItems" -> setUniqueItems(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(ref != null, "$ref")
			.addIf(collectionFormat != null, "collectionFormat")
			.addIf(_default != null, "default")
			.addIf(_enum != null, "enum")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(format != null, "format")
			.addIf(items != null, "items")
			.addIf(maxItems != null, "maxItems")
			.addIf(maxLength != null, "maxLength")
			.addIf(maximum != null, "maximum")
			.addIf(minItems != null, "minItems")
			.addIf(minLength != null, "minLength")
			.addIf(minimum != null, "minimum")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(pattern != null, "pattern")
			.addIf(type != null, "type")
			.addIf(uniqueItems != null, "uniqueItems")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param openApi The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public Items resolveRefs(OpenApi openApi, Deque<String> refStack, int maxDepth) {

		if (ref != null) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = openApi.findRef(ref, Items.class);
			r = r.resolveRefs(openApi, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		set("properties", resolveRefs(get("properties"), openApi, refStack, maxDepth));

		if (items != null)
			items = items.resolveRefs(openApi, refStack, maxDepth);

		set("example", null);

		return this;
	}

	/* Resolve references in extra attributes */
	private Object resolveRefs(Object o, OpenApi openApi, Deque<String> refStack, int maxDepth) {
		if (o instanceof JsonMap om) {
			var ref2 = om.get("$ref");
			if (ref2 instanceof CharSequence) {
				var sref = ref2.toString();
				if (refStack.contains(sref) || refStack.size() >= maxDepth)
					return o;
				refStack.addLast(sref);
				var o2 = openApi.findRef(sref, Object.class);
				o2 = resolveRefs(o2, openApi, refStack, maxDepth);
				refStack.removeLast();
				return o2;
			}
			for (var e : om.entrySet())
				e.setValue(resolveRefs(e.getValue(), openApi, refStack, maxDepth));
		}
		if (o instanceof JsonList x)
			for (var li = x.listIterator(); li.hasNext();)
				li.set(resolveRefs(li.next(), openApi, refStack, maxDepth));
		return o;
	}
}