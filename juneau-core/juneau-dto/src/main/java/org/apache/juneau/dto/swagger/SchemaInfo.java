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
import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * The Schema Object allows the definition of input and output data types.
 * 
 * <p>
 * These types can be objects, but also primitives and arrays.
 * This object is based on the JSON Schema Specification Draft 4 and uses a predefined subset of it.
 * On top of this subset, there are extensions provided by this specification to allow for more complete documentation.
 * 
 * <p>
 * Further information about the properties can be found in JSON Schema Core and JSON Schema Validation.
 * Unless stated otherwise, the property definitions follow the JSON Schema specification as referenced here.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	SchemaInfo x = <jsm>schemaInfo</jsm>()
 * 		.type("string")
 * 		.title("foo")
 * 
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 * 
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"type"</js>: <js>"string"</js>,
 * 		<js>"title"</js>: <js>"foo"</js>
 * 	}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview &gt; juneau-dto &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,enum,type,items,allOf,properties,additionalProperties,discriminator,readOnly,xml,externalDocs,example,*")
public class SchemaInfo extends SwaggerElement {

	private String 
		format,
		title,
		description,
		pattern,
		type,
		discriminator;
	private Number 
		multipleOf,
		maximum,
		minimum;
	private Integer 
		maxLength,
		minLength,
		maxItems,
		minItems,
		maxProperties,
		minProperties;
	private Boolean 
		exclusiveMaximum,
		exclusiveMinimum,
		uniqueItems,
		required,
		readOnly;
	private Object 
		_default,
		example;
	private Items items;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private List<Object> 
		_enum,
		allOf;
	private Map<String,Map<String,Object>> properties;
	private Map<String,Object> additionalProperties;

	/**
	 * Bean property getter:  <property>format</property>.
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a>
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setFormat(String value) {
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
	public SchemaInfo format(Object value) {
		return setFormat(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Same as {@link #setTitle(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo title(Object value) {
		return setTitle(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDescription(String value) {
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
	public SchemaInfo description(Object value) {
		return setDescription(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 * 
	 * <p>
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
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
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Same as {@link #setDefault(Object)}.
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _default(Object value) {
		return setDefault(value);
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMultipleOf(Number value) {
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
	public SchemaInfo multipleOf(Object value) {
		return setMultipleOf(toNumber(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaximum(Number value) {
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
	public SchemaInfo maximum(Object value) {
		return setMaximum(toNumber(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExclusiveMaximum(Boolean value) {
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
	public SchemaInfo exclusiveMaximum(Object value) {
		return setExclusiveMaximum(toBoolean(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinimum(Number value) {
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
	public SchemaInfo minimum(Object value) {
		return setMinimum(toNumber(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExclusiveMinimum(Boolean value) {
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
	public SchemaInfo exclusiveMinimum(Object value) {
		return setExclusiveMinimum(toBoolean(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxLength(Integer value) {
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
	public SchemaInfo maxLength(Object value) {
		return setMaxLength(toInteger(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinLength(Integer value) {
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
	public SchemaInfo minLength(Object value) {
		return setMinLength(toInteger(value));
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
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setPattern(String value) {
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
	public SchemaInfo pattern(Object value) {
		return setPattern(toStringVal(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxItems(Integer value) {
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
	public SchemaInfo maxItems(Object value) {
		return setMaxItems(toInteger(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinItems(Integer value) {
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
	public SchemaInfo minItems(Object value) {
		return setMinItems(toInteger(value));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setUniqueItems(Boolean value) {
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
	public SchemaInfo uniqueItems(Object value) {
		return setUniqueItems(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Bean property setter:  <property>maxProperties</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxProperties(Integer value) {
		maxProperties = value;
		return this;
	}

	/**
	 * Same as {@link #setMaxProperties(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxProperties(Object value) {
		return setMaxProperties(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinProperties() {
		return minProperties;
	}

	/**
	 * Bean property setter:  <property>minProperties</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinProperties(Integer value) {
		minProperties = value;
		return this;
	}

	/**
	 * Same as {@link #setMinProperties(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minProperties(Object value) {
		return setMinProperties(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * Same as {@link #setRequired(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo required(Object value) {
		return setRequired(toBoolean(value));
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
	public SchemaInfo setEnum(Collection<Object> value) {
		_enum = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 * 
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addEnum(Collection<Object> value) {
		_enum = addToList(_enum, value);
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _enum(Object...values) {
		_enum = addToList(_enum, values, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setType(String value) {
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
	public SchemaInfo type(Object value) {
		return setType(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setItems(Items value) {
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
	 * 			<p class='bcode'>
	 * 	items(<js>"{type:'type',format:'format',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo items(Object value) {
		return setItems(toType(value, Items.class));
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getAllOf() {
		return allOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setAllOf(Collection<Object> value) {
		allOf = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>allOf</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addAllOf(Collection<Object> values) {
		allOf = addToList(allOf, values);
		return this;
	}

	/**
	 * Adds one or more values to the <property>allOf</property> property.
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
	 * 	allOf(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	allOf(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo allOf(Object...values) {
		allOf = addToList(allOf, values, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Map<String,Object>> getProperties() {
		return properties;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setProperties(Map<String,Map<String,Object>> value) {
		properties = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>properties</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addProperties(Map<String,Map<String,Object>> values) {
		properties = addToMap(properties, values);
		return this;
	}

	/**
	 * Adds one or more values to the <property>properties</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,Map&lt;String,Object&gt;&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,Map&lt;String,Object&gt;&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	properties(<js>"{name:{foo:'bar'}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SchemaInfo properties(Object...values) {
		properties = addToMap((Map)properties, values, String.class, Map.class, String.class, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getAdditionalProperties() {
		return additionalProperties;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setAdditionalProperties(Map<String,Object> value) {
		additionalProperties = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>additionalProperties</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addAdditionalProperties(Map<String,Object> values) {
		additionalProperties = addToMap(additionalProperties, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>additionalProperties</property> property.
	 * 
	 * @param name The extra property name.
	 * @param value The extra property value.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo additionalProperty(String name, Object value) {
		additionalProperties = addToMap(additionalProperties, name, value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>properties</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,Object&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,Object&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	extraProperties(<js>"{name:'value'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo additionalProperties(Object...values) {
		additionalProperties = addToMap(additionalProperties, values, String.class, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>discriminator</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDiscriminator() {
		return discriminator;
	}

	/**
	 * Bean property setter:  <property>discriminator</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDiscriminator(String value) {
		discriminator = value;
		return this;
	}

	/**
	 * Same as {@link #setDiscriminator(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo discriminator(Object value) {
		return setDiscriminator(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>readOnly</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getReadOnly() {
		return readOnly;
	}

	/**
	 * Bean property setter:  <property>readOnly</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setReadOnly(Boolean value) {
		readOnly = value;
		return this;
	}

	/**
	 * Same as {@link #setReadOnly(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo readOnly(Object value) {
		return setReadOnly(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>xml</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Xml getXml() {
		return xml;
	}

	/**
	 * Bean property setter:  <property>xml</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setXml(Xml value) {
		xml = value;
		return this;
	}

	/**
	 * Same as {@link #setXml(Xml)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Xml}
	 * 		<li><code>String</code> - JSON object representation of {@link Xml}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	xml(<js>"{name:'name',namespace:'namespace',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo xml(Object value) {
		return setXml(toType(value, Xml.class));
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
	}

	/**
	 * Same as {@link #setExternalDocs(ExternalDocumentation)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link ExternalDocumentation}
	 * 		<li><code>String</code> - JSON object representation of {@link ExternalDocumentation}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo externalDocs(Object value) {
		return setExternalDocs(toType(value, ExternalDocumentation.class));
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Same as {@link #setExample(Object)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo example(Object value) {
		return setExample(value);
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "format": return toType(getFormat(), type);
			case "title": return toType(getTitle(), type);
			case "description": return toType(getDescription(), type);
			case "default": return toType(getDefault(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
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
			case "maxProperties": return toType(getMaxProperties(), type);
			case "minProperties": return toType(getMinProperties(), type);
			case "required": return toType(getRequired(), type);
			case "enum": return toType(getEnum(), type);
			case "type": return toType(getType(), type);
			case "items": return toType(getItems(), type);
			case "allOf": return toType(getAllOf(), type);
			case "properties": return toType(getProperties(), type);
			case "additionalProperties": return toType(getAdditionalProperties(), type);
			case "discriminator": return toType(getDiscriminator(), type);
			case "readOnly": return toType(getReadOnly(), type);
			case "xml": return toType(getXml(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "example": return toType(getExample(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SchemaInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "format": return format(value);
			case "title": return title(value);
			case "description": return description(value);
			case "default": return _default(value);
			case "multipleOf": return multipleOf(value);
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
			case "maxProperties": return maxProperties(value);
			case "minProperties": return minProperties(value);
			case "required": return required(value);
			case "enum": return setEnum(null)._enum(value);
			case "type": return type(value);
			case "items": return items(value);
			case "allOf": return setAllOf(null).allOf(value);
			case "properties": return setProperties(null).properties(value);
			case "additionalProperties": return setAdditionalProperties(null).additionalProperties(value);
			case "discriminator": return discriminator(value);
			case "readOnly": return readOnly(value);
			case "xml": return xml(value);
			case "externalDocs": return externalDocs(value);
			case "example": return example(value);
			default: 
				super.set(property, value);
				return this;
		}
	}
}
