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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo <jv>headerInfo</jv> = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>headerInfo</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>headerInfo</jv>.toString();
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoSwagger}
 * </ul>
 */
@Bean(properties="description,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref,example,*")
@SuppressWarnings({"unchecked"})
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
	private Object _default;
	private Set<Object> _enum;
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
		this._enum = newSet(copyFrom._enum);
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

	@Override /* SwaggerElement */
	protected HeaderInfo strict() {
		super.strict();
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// collectionFormat
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
	 */
	public void setCollectionFormat(String value) {
		if (isStrict() && ! ArrayUtils.contains(value, VALID_COLLECTION_FORMATS))
			throw new BasicRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, VALID_COLLECTION_FORMATS
			);
		collectionFormat = value;
	}

	/**
	 * Bean property fluent getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> collectionFormat() {
		return Optional.ofNullable(getCollectionFormat());
	}

	/**
	 * Bean property fluent setter:  <property>collectionFormat</property>.
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo collectionFormat(String value) {
		setCollectionFormat(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// default
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
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
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setDefault(Object value) {
		_default = value;
	}

	/**
	 * Bean property fluent getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Object> _default() {
		return Optional.ofNullable(getDefault());
	}

	/**
	 * Bean property fluent setter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _default(Object value) {
		setDefault(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> description() {
		return Optional.ofNullable(getDescription());
	}

	/**
	 * Bean property fluent setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// enum
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setEnum(Collection<Object> value) {
		_enum = newSet(value);
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo addEnum(Collection<Object> value) {
		_enum = setBuilder(_enum).sparse().addAll(value).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>enum</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<Object>> _enum() {
		return Optional.ofNullable(getEnum());
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The values to set on this property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _enum(Collection<Object> value) {
		setEnum(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	_enumJson(<js>"['foo','bar']"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _enum(Object...value) {
		setEnum(setBuilder(Object.class).sparse().addAny(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// example
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExample(Object value) {
		example = value;
	}

	/**
	 * Bean property fluent getter:  <property>example</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Object> example() {
		return Optional.ofNullable(getExample());
	}

	/**
	 * Bean property fluent setter:  <property>example</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo example(Object value) {
		setExample(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// exclusiveMaximum
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
	}

	/**
	 * Bean property fluent getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> exclusiveMaximum() {
		return Optional.ofNullable(getExclusiveMaximum());
	}

	/**
	 * Bean property fluent setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMaximum(Boolean value) {
		setExclusiveMaximum(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMaximum(String value) {
		setExclusiveMaximum(toBoolean(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// exclusiveMinimum
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
	}

	/**
	 * Bean property fluent getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> exclusiveMinimum() {
		return Optional.ofNullable(getExclusiveMinimum());
	}

	/**
	 * Bean property fluent setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMinimum(Boolean value) {
		setExclusiveMinimum(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMinimum(String value) {
		setExclusiveMinimum(toBoolean(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// format
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setFormat(String value) {
		format = value;
	}

	/**
	 * Bean property fluent getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> format() {
		return Optional.ofNullable(getFormat());
	}

	/**
	 * Bean property fluent setter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo format(String value) {
		setFormat(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// items
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setItems(Items value) {
		items = value;
	}

	/**
	 * Bean property fluent getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Items> items() {
		return Optional.ofNullable(getItems());
	}

	/**
	 * Bean property fluent setter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required if <c>type</c> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo items(Items value) {
		setItems(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	items(<js>"{type:'type',format:'format',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Property value is required if <c>type</c> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo items(String json) {
		setItems(toType(json, Items.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// maximum
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMaximum(Number value) {
		maximum = value;
	}

	/**
	 * Bean property fluent getter:  <property>maximum</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Number> maximum() {
		return Optional.ofNullable(getMaximum());
	}

	/**
	 * Bean property fluent setter:  <property>maximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maximum(Number value) {
		setMaximum(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>maximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maximum(String value) {
		setMaximum(toNumber(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// maxItems
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMaxItems(Integer value) {
		maxItems = value;
	}

	/**
	 * Bean property fluent getter:  <property>maxItems</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> maxItems() {
		return Optional.ofNullable(getMaxItems());
	}

	/**
	 * Bean property fluent setter:  <property>maxItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxItems(Integer value) {
		setMaxItems(toInteger(value));
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>maxItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxItems(String value) {
		setMaxItems(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// maxLength
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMaxLength(Integer value) {
		maxLength = value;
	}

	/**
	 * Bean property fluent getter:  <property>maxLength</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> maxLength() {
		return Optional.ofNullable(getMaxLength());
	}

	/**
	 * Bean property fluent setter:  <property>maxLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxLength(Integer value) {
		setMaxLength(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>maxLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxLength(String value) {
		setMaxLength(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// minimum
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMinimum(Number value) {
		minimum = value;
	}

	/**
	 * Bean property fluent getter:  <property>minimum</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Number> minimum() {
		return Optional.ofNullable(getMinimum());
	}

	/**
	 * Bean property fluent setter:  <property>minimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minimum(Number value) {
		setMinimum(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>minimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minimum(String value) {
		setMinimum(toNumber(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// minItems
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMinItems(Integer value) {
		minItems = value;
	}

	/**
	 * Bean property fluent getter:  <property>minItems</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> minItems() {
		return Optional.ofNullable(getMinItems());
	}

	/**
	 * Bean property fluent setter:  <property>minItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minItems(Integer value) {
		setMinItems(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>minItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minItems(String value) {
		setMinItems(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// minLength
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMinLength(Integer value) {
		minLength = value;
	}

	/**
	 * Bean property fluent getter:  <property>minLength</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> minLength() {
		return Optional.ofNullable(getMinLength());
	}

	/**
	 * Bean property fluent setter:  <property>minLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minLength(Integer value) {
		setMinLength(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>minLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minLength(String value) {
		setMinLength(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// multipleOf
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMultipleOf(Number value) {
		multipleOf = value;
	}

	/**
	 * Bean property fluent getter:  <property>multipleOf</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Number> multipleOf() {
		return Optional.ofNullable(getMultipleOf());
	}

	/**
	 * Bean property fluent setter:  <property>multipleOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo multipleOf(Number value) {
		setMultipleOf(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>multipleOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo multipleOf(String value) {
		setMultipleOf(toNumber(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// pattern
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setPattern(String value) {
		pattern = value;
	}

	/**
	 * Bean property fluent getter:  <property>pattern</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> pattern() {
		return Optional.ofNullable(getPattern());
	}

	/**
	 * Bean property fluent setter:  <property>pattern</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This string SHOULD be a valid regular expression.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo pattern(String value) {
		setPattern(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// $ref
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	@Beanp("$ref")
	public void setRef(String value) {
		ref = value;
	}

	/**
	 * Bean property fluent getter:  <property>$ref</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> ref() {
		return Optional.ofNullable(getRef());
	}

	/**
	 * Bean property fluent setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo ref(String value) {
		setRef(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 */
	public void setType(String value) {
		if (isStrict() && ! ArrayUtils.contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
	}

	/**
	 * Bean property fluent getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> type() {
		return Optional.ofNullable(getType());
	}

	/**
	 * Bean property fluent setter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HeaderInfo type(String value) {
		setType(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// uniqueItems
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setUniqueItems(Boolean value) {
		uniqueItems = value;
	}

	/**
	 * Bean property fluent getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> uniqueItems() {
		return Optional.ofNullable(getUniqueItems());
	}

	/**
	 * Bean property fluent setter:  <property>uniqueItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo uniqueItems(Boolean value) {
		setUniqueItems(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>uniqueItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo uniqueItems(String value) {
		setUniqueItems(toBoolean(value));
		return this;
	}


	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "collectionFormat": return toType(getCollectionFormat(), type);
			case "default": return toType(getDefault(), type);
			case "description": return (T)getDescription();
			case "enum": return toType(getEnum(), type);
			case "example": return toType(getExample(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "format": return toType(getFormat(), type);
			case "items": return toType(getItems(), type);
			case "maximum": return toType(getMaximum(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "minimum": return toType(getMinimum(), type);
			case "minItems": return toType(getMinItems(), type);
			case "minLength": return toType(getMinLength(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "pattern": return toType(getPattern(), type);
			case "$ref": return toType(getRef(), type);
			case "type": return toType(getType(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
		default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public HeaderInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "collectionFormat": return collectionFormat(stringify(value));
			case "default": return _default(value);
			case "description": return description(stringify(value));
			case "enum": return _enum(value);
			case "example": return example(value);
			case "exclusiveMaximum": return exclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum": return exclusiveMinimum(toBoolean(value));
			case "format": return format(stringify(value));
			case "items": return items(toType(value, Items.class));
			case "maximum": return maximum(toNumber(value));
			case "maxItems": return maxItems(toInteger(value));
			case "maxLength": return maxLength(toInteger(value));
			case "minimum": return minimum(toNumber(value));
			case "minItems": return minItems(toInteger(value));
			case "minLength": return minLength(toInteger(value));
			case "multipleOf": return multipleOf(toNumber(value));
			case "pattern": return pattern(stringify(value));
			case "$ref": return ref(stringify(value));
			case "type": return type(stringify(value));
			case "uniqueItems": return uniqueItems(toBoolean(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(collectionFormat != null, "collectionFormat")
			.appendIf(_default != null, "default")
			.appendIf(description != null, "description")
			.appendIf(_enum != null, "enum")
			.appendIf(example != null, "example")
			.appendIf(exclusiveMaximum != null, "exclusiveMaximum")
			.appendIf(exclusiveMinimum != null, "exclusiveMinimum")
			.appendIf(format != null, "format")
			.appendIf(items != null, "items")
			.appendIf(maximum != null, "maximum")
			.appendIf(maxItems != null, "maxItems")
			.appendIf(maxLength != null, "maxLength")
			.appendIf(minimum != null, "minimum")
			.appendIf(minItems != null, "minItems")
			.appendIf(minLength != null, "minLength")
			.appendIf(multipleOf != null, "multipleOf")
			.appendIf(pattern != null, "pattern")
			.appendIf(ref != null, "$ref")
			.appendIf(type != null, "type")
			.appendIf(uniqueItems != null, "uniqueItems");
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
			HeaderInfo r = swagger.findRef(ref, HeaderInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}
}
