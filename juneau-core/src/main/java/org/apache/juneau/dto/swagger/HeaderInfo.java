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

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 */
@Bean(properties="description,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf")
@SuppressWarnings({"hiding", "unchecked"})
public class HeaderInfo extends SwaggerElement {

	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv","ssv","tsv","pipes","multi"};

	private String description;
	private String type;
	private String format;
	private Items items;
	private String collectionFormat;
	private Object _default;
	private Number maximum;
	private Boolean exclusiveMaximum;
	private Number minimum;
	private Boolean exclusiveMinimum;
	private Integer maxLength;
	private Integer minLength;
	private String pattern;
	private Integer maxItems;
	private Integer minItems;
	private Boolean uniqueItems;
	private List<Object> _enum;
	private Number multipleOf;

	@Override /* SwaggerElement */
	protected HeaderInfo strict() {
		super.strict();
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A short description of the header.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A short description of the header.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Synonym for {@link #description(String)}.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * <p>
	 * Required. The type of the object.
	 * The value MUST be one of <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, or <js>"array"</js>.
	 *
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * <p>
	 * Required. The type of the object.
	 * The value MUST be one of <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, or <js>"array"</js>.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setType(String type) {
		if (isStrict() && ! ArrayUtils.contains(type, VALID_TYPES))
			throw new RuntimeException("Invalid value passed in to setType(String).  Value='"+type+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_TYPES));
		this.type = type;
		return this;
	}

	/**
	 * Synonym for {@link #setType(String)}.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo type(String type) {
		return setType(type);
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 * <p>
	 * The extending format for the previously mentioned <code>type</code>. See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further details.
	 *
	 * @return The value of the <property>format</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 * <p>
	 * The extending format for the previously mentioned <code>type</code>. See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further details.
	 *
	 * @param format The new value for the <property>format</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setFormat(String format) {
		this.format = format;
		return this;
	}

	/**
	 * Synonym for {@link #setFormat(String)}.
	 *
	 * @param format The new value for the <property>format</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo format(String format) {
		return setFormat(format);
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>.
	 * Describes the type of items in the array.
	 *
	 * @return The value of the <property>items</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>.
	 * Describes the type of items in the array.
	 *
	 * @param items The new value for the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setItems(Items items) {
		this.items = items;
		return this;
	}

	/**
	 * Synonym for {@link #setItems(Items)}.
	 *
	 * @param items The new value for the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo items(Items items) {
		return setItems(items);
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 * <p>
	 * Determines the format of the array if type array is used.
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><code>csv</code> - comma separated values <code>foo,bar</code>.
	 * 	<li><code>ssv</code> - space separated values <code>foo bar</code>.
	 * 	<li><code>tsv</code> - tab separated values <code>foo\tbar</code>.
	 * 	<li><code>pipes</code> - pipe separated values <code>foo|bar</code>.
	 * </ul>
	 * <p>
	 * Default value is <code>csv</code>.
	 *
	 * @return The value of the <property>collectionFormat</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 * <p>
	 * Determines the format of the array if type array is used.
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><code>csv</code> - comma separated values <code>foo,bar</code>.
	 * 	<li><code>ssv</code> - space separated values <code>foo bar</code>.
	 * 	<li><code>tsv</code> - tab separated values <code>foo\tbar</code>.
	 * 	<li><code>pipes</code> - pipe separated values <code>foo|bar</code>.
	 * </ul>
	 * <p>
	 * Default value is <code>csv</code>.
	 *
	 * @param collectionFormat The new value for the <property>collectionFormat</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setCollectionFormat(String collectionFormat) {
		if (isStrict() && ! ArrayUtils.contains(collectionFormat, VALID_COLLECTION_FORMATS))
			throw new RuntimeException("Invalid value passed in to setCollectionFormat(String).  Value='"+collectionFormat+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_COLLECTION_FORMATS));
		this.collectionFormat = collectionFormat;
		return this;
	}

	/**
	 * Synonym for {@link #setCollectionFormat(String)}.
	 *
	 * @param collectionFormat The new value for the <property>collectionFormat</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo collectionFormat(String collectionFormat) {
		return setCollectionFormat(collectionFormat);
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 * (Note: <js>"default"</js> has no meaning for required items.)
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>.
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the header.
	 *
	 * @return The value of the <property>_default</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() {
		return _default;
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 * <p>
	 * Declares the value of the header that the server will use if none is provided.
	 * (Note: <js>"default"</js> has no meaning for required items.)
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>.
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the header.
	 *
	 * @param _default The new value for the <property>_default</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDefault(Object _default) {
		this._default = _default;
		return this;
	}

	/**
	 * Synonym for {@link #setDefault(Object)}.
	 *
	 * @param _default The new value for the <property>_default</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _default(Object _default) {
		return setDefault(_default);
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @return The value of the <property>maximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaximum(Number maximum) {
		this.maximum = maximum;
		return this;
	}

	/**
	 * Synonym for {@link #setMaximum(Number)}.
	 *
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maximum(Number maximum) {
		return setMaximum(maximum);
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @return The value of the <property>exclusiveMaximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	/**
	 * Synonym for {@link #setExclusiveMaximum(Boolean)}.
	 *
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMaximum(Boolean exclusiveMaximum) {
		return setExclusiveMaximum(exclusiveMaximum);
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @return The value of the <property>minimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinimum(Number minimum) {
		this.minimum = minimum;
		return this;
	}

	/**
	 * Synonym for {@link #setMinimum(Number)}.
	 *
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minimum(Number minimum) {
		return setMinimum(minimum);
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @return The value of the <property>exclusiveMinimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	/**
	 * Synonym for {@link #setExclusiveMinimum(Boolean)}.
	 *
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo exclusiveMinimum(Boolean exclusiveMinimum) {
		return setExclusiveMinimum(exclusiveMinimum);
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>.
	 *
	 * @return The value of the <property>maxLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>.
	 *
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	/**
	 * Synonym for {@link #setMaxLength(Integer)}.
	 *
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxLength(Integer maxLength) {
		return setMaxLength(maxLength);
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>.
	 *
	 * @return The value of the <property>minLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>.
	 *
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinLength(Integer minLength) {
		this.minLength = minLength;
		return this;
	}

	/**
	 *
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minLength(Integer minLength) {
		return setMinLength(minLength);
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>.
	 *
	 * @return The value of the <property>pattern</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>.
	 *
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Synonym for {@link #setPattern(String)}.
	 *
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo pattern(String pattern) {
		return setPattern(pattern);
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>.
	 *
	 * @return The value of the <property>maxItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>.
	 *
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * Synonym for {@link #setMaxItems(Integer)}.
	 *
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo maxItems(Integer maxItems) {
		return setMaxItems(maxItems);
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>.
	 *
	 * @return The value of the <property>minItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>.
	 *
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMinItems(Integer minItems) {
		this.minItems = minItems;
		return this;
	}

	/**
	 * Synonym for {@link #setMinItems(Integer)}.
	 *
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo minItems(Integer minItems) {
		return setMinItems(minItems);
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>.
	 *
	 * @return The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>.
	 *
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
		return this;
	}

	/**
	 * Synonym for {@link #setUniqueItems(Boolean)}.
	 *
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo uniqueItems(Boolean uniqueItems) {
		return setUniqueItems(uniqueItems);
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setEnum(List<Object> _enum) {
		this._enum = _enum;
		return this;
	}

	/**
	 * Bean property adder:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @param _enum The new values to add to the <property>enum</property> property on this bean.
	 * 	These can either be individual objects or {@link Collection Collections} of objects.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo addEnum(Object..._enum) {
		for (Object o  : _enum) {
			if (o != null) {
				if (o instanceof Collection)
					addEnum((Collection<Object>)o);
				else {
					if (this._enum == null)
						this._enum = new LinkedList<Object>();
					this._enum.add(o);
				}
			}
		}
		return this;
	}

	/**
	 * Synonym for {@link #addEnum(Object...)}.
	 *
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo _enum(Object..._enum) {
		return addEnum(_enum);
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>.
	 *
	 * @return The value of the <property>multipleOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>.
	 *
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
		return this;
	}

	/**
	 * Synonym for {@link #setMultipleOf(Number)}.
	 *
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo multipleOf(Number multipleOf) {
		return setMultipleOf(multipleOf);
	}
}
