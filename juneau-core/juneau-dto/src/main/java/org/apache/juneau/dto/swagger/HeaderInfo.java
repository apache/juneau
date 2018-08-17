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

import static org.apache.juneau.internal.BeanPropertyUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo x = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Swagger}
 * </ul>
 */
@Bean(properties="description,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref,x-example,*")
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
	private List<Object> _enum;
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

		this.description = copyFrom.description;
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
		this._default = copyFrom._default;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this._enum = newList(copyFrom._enum);
		this.example = copyFrom.example;
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo description(Object value) {
		return setDescription(toStringVal(value));
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
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
	public HeaderInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new FormattedRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
		return this;
	}

	/**
	 * Same as {@link #setType(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo type(Object value) {
		return setType(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <code>type</code>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a>
	 * </ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setFormat(String value) {
		format = value;
		return this;
	}

	/**
	 * Same as {@link #setFormat(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo format(Object value) {
		return setFormat(toStringVal(value));
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setItems(Items value) {
		items = value;
		return this;
	}

	/**
	 * Same as {@link #setItems(Items)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Items}
	 * 		<li><code>String</code> - JSON object representation of {@link Items}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	items(<js>"{type:'type',format:'format',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo items(Object value) {
		return setItems(toType(value, Items.class));
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw new FormattedRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, VALID_COLLECTION_FORMATS
			);
		collectionFormat = value;
		return this;
	}

	/**
	 * Same as {@link #setCollectionFormat(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo collectionFormat(Object value) {
		return setCollectionFormat(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the header.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the header.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Same as {@link #setDefault(Object)}.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _default(Object value) {
		return setDefault(value);
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaximum(Number value) {
		maximum = value;
		return this;
	}

	/**
	 * Same as {@link #setMaximum(Number)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maximum(Object value) {
		return setMaximum(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Same as {@link #setExclusiveMaximum(Boolean)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMaximum(Object value) {
		return setExclusiveMaximum(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinimum(Number value) {
		minimum = value;
		return this;
	}

	/**
	 * Same as {@link #setMinimum(Number)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minimum(Object value) {
		return setMinimum(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Same as {@link #setExclusiveMinimum(Boolean)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMinimum(Object value) {
		return setExclusiveMinimum(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaxLength(Integer value) {
		maxLength = value;
		return this;
	}

	/**
	 * Same as {@link #setMaxLength(Integer)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxLength(Object value) {
		return setMaxLength(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinLength(Integer value) {
		minLength = value;
		return this;
	}

	/**
	 * Same as {@link #setMinLength(Integer)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minLength(Object value) {
		return setMinLength(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>
	 * </ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setPattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Same as {@link #setPattern(String)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo pattern(Object value) {
		return setPattern(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaxItems(Integer value) {
		maxItems = value;
		return this;
	}

	/**
	 * Same as {@link #setMaxItems(Integer)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxItems(Object value) {
		return setMaxItems(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinItems(Integer value) {
		minItems = value;
		return this;
	}

	/**
	 * Same as {@link #setMinItems(Integer)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minItems(Object value) {
		return setMinItems(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	/**
	 * Same as {@link #setUniqueItems(Boolean)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo uniqueItems(Object value) {
		return setUniqueItems(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setEnum(Collection<Object> value) {
		_enum = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo addEnum(Collection<Object> values) {
		_enum = addToList(_enum, values);
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
	 * 		<li><code>String</code> - JSON array representation of <code>Collection&lt;Object&gt;</code>.
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	_enum(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values.
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	_enum(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _enum(Object...values) {
		_enum = addToList(_enum, values, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMultipleOf(Number value) {
		multipleOf = value;
		return this;
	}

	/**
	 * Same as {@link #setMultipleOf(Number)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo multipleOf(Object value) {
		return setMultipleOf(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("$ref")
	public String getRef() {
		return ref;
	}

	/**
	 * Returns <jk>true</jk> if this object has a <js>"$ref"</js> attribute.
	 *
	 * @return <jk>true</jk> if this object has a <js>"$ref"</js> attribute.
	 */
	public boolean hasRef() {
		return ref != null;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("$ref")
	public HeaderInfo setRef(Object value) {
		ref = StringUtils.asString(value);
		return this;
	}

	/**
	 * Same as {@link #setRef(Object)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo ref(Object value) {
		return setRef(value);
	}

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("x-example")
	public Object getExample() {
		return example;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("x-example")
	public HeaderInfo setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo example(Object value) {
		example = value;
		return this;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return (T)getDescription();
			case "type": return toType(getType(), type);
			case "format": return toType(getFormat(), type);
			case "items": return toType(getItems(), type);
			case "collectionFormat": return toType(getCollectionFormat(), type);
			case "$ref": return toType(getRef(), type);
			case "default": return toType(getDefault(), type);
			case "maximum": return toType(getMaximum(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "minimum": return toType(getMinimum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "minLength": return toType(getMinLength(), type);
			case "pattern": return toType(getPattern(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "minItems": return toType(getMinItems(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
			case "enum": return toType(getEnum(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "x-example": return toType(getExample(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public HeaderInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return description(value);
			case "type": return type(value);
			case "format": return format(value);
			case "items": return items(value);
			case "collectionFormat": return collectionFormat(value);
			case "$ref": return ref(value);
			case "default": return _default(value);
			case "maximum": return maximum(value);
			case "exclusiveMaximum": return exclusiveMaximum(value);
			case "minimum": return minimum(value);
			case "exclusiveMinimum": return exclusiveMinimum(value);
			case "maxLength": return maxLength(value);
			case "minLength": return minLength(value);
			case "pattern": return pattern(value);
			case "maxItems": return maxItems(value);
			case "minItems": return minItems(value);
			case "uniqueItems": return uniqueItems(value);
			case "enum": return setEnum(null)._enum(value);
			case "multipleOf": return multipleOf(value);
			case "x-example": return example(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = new ASet<String>()
			.appendIf(description != null, "description")
			.appendIf(type != null, "type")
			.appendIf(format != null, "format")
			.appendIf(items != null, "items")
			.appendIf(collectionFormat != null, "collectionFormat")
			.appendIf(ref != null, "$ref")
			.appendIf(_default != null, "default")
			.appendIf(maximum != null, "maximum")
			.appendIf(exclusiveMaximum != null, "exclusiveMaximum")
			.appendIf(minimum != null, "minimum")
			.appendIf(exclusiveMinimum != null, "exclusiveMinimum")
			.appendIf(maxLength != null, "maxLength")
			.appendIf(minLength != null, "minLength")
			.appendIf(pattern != null, "pattern")
			.appendIf(maxItems != null, "maxItems")
			.appendIf(minItems != null, "minItems")
			.appendIf(uniqueItems != null, "uniqueItems")
			.appendIf(_enum != null, "enum")
			.appendIf(multipleOf != null, "multipleOf")
			.appendIf(example != null, "example");
		return new MultiSet<>(s, super.keySet());

	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param swagger The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
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
