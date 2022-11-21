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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.dto.swagger.ExternalDocumentation;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.Xml;
import org.apache.juneau.internal.*;

import java.util.*;

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
 */
@Bean(properties="format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,enum,type,items,allOf,anyOf,oneOf,properties,additionalProperties,not,discriminator,readOnly,writeOnly,nullable,deprecated,xml,externalDocs,example,$ref,*")
@FluentSetters
public class SchemaInfo extends OpenApiElement {

	private String
		format,
		title,
		description,
		pattern,
		ref,
		type;
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
		nullable,
		writeOnly,
		readOnly,
		deprecated;
	private Object
		_default,
		example;
	private Items items;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private List<Object>
			allOf,
			oneOf,
			anyOf,
			_enum;
	private List<String>
		required;
	private Discriminator discriminator;
	private Map<String, SchemaInfo> properties;
	private SchemaInfo additionalProperties;
	private SchemaInfo not;

	/**
	 * Default constructor.
	 */
	public SchemaInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public SchemaInfo(SchemaInfo copyFrom) {
		super(copyFrom);

		this.format = copyFrom.format;
		this.title = copyFrom.title;
		this.description = copyFrom.description;
		this.ref = copyFrom.ref;
		this.nullable = copyFrom.nullable;
		this.writeOnly = copyFrom.writeOnly;
		this.deprecated = copyFrom.deprecated;
		this.pattern = copyFrom.pattern;
		this.type = copyFrom.type;
		this.discriminator = copyFrom.discriminator;
		this.multipleOf = copyFrom.multipleOf;
		this.maximum = copyFrom.maximum;
		this.minimum = copyFrom.minimum;
		this.maxLength = copyFrom.maxLength;
		this.minLength = copyFrom.minLength;
		this.maxItems = copyFrom.maxItems;
		this.minItems = copyFrom.minItems;
		this.maxProperties = copyFrom.maxProperties;
		this.minProperties = copyFrom.minProperties;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.uniqueItems = copyFrom.uniqueItems;
		this.readOnly = copyFrom.readOnly;
		this._default = copyFrom._default;
		this.example = copyFrom.example;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.xml = copyFrom.xml == null ? null : copyFrom.xml.copy();
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this._enum = copyOf(copyFrom._enum);
		this.allOf = copyOf(copyFrom.allOf);
		this.required = copyOf(copyFrom.required);
		this.anyOf = copyOf(copyFrom.anyOf);
		this.oneOf = copyOf(copyFrom.oneOf);

		if (copyFrom.properties == null) {
			this.properties = null;
		} else {
			this.properties = new LinkedHashMap<>();
			for (Map.Entry<String, SchemaInfo> e : copyFrom.properties.entrySet())
				this.properties.put(e.getKey(), e.getValue().copy());
		}

		this.additionalProperties = copyFrom.additionalProperties == null ? null : copyFrom.additionalProperties.copy();
		this.not = copyFrom.not == null ? null : copyFrom.not.copy();
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SchemaInfo copy() {
		return new SchemaInfo(this);
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * 	<br>Formats defined by the OAS include:
	 * 	<ul>
	 * 		<li><js>"int32"</js>
	 * 		<li><js>"int64"</js>
	 * 		<li><js>"float"</js>
	 * 		<li><js>"double"</js>
	 * 		<li><js>"byte"</js>
	 * 		<li><js>"binary"</js>
	 * 		<li><js>"date"</js>
	 * 		<li><js>"date-time"</js>
	 * 		<li><js>"password"</js>
	 * 	</ul>
	 * @return This object
	 */
	public SchemaInfo setFormat(String value) {
		format = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setTitle(String value) {
		title = value;
		return this;
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
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setDescription(String value) {
		description = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setDefault(Object value) {
		_default = value;
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
	public SchemaInfo setMultipleOf(Number value) {
		multipleOf = value;
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
	public SchemaInfo setMaximum(Number value) {
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
	public SchemaInfo setExclusiveMaximum(Boolean value) {
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
	public SchemaInfo setMinimum(Number value) {
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
	public SchemaInfo setExclusiveMinimum(Boolean value) {
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
	public SchemaInfo setMaxLength(Integer value) {
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
	public SchemaInfo setMinLength(Integer value) {
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
	public SchemaInfo setPattern(String value) {
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
	public SchemaInfo setMaxItems(Integer value) {
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
	public SchemaInfo setMinItems(Integer value) {
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
	public SchemaInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getNullable() {
		return nullable;
	}
	/**
	 * Bean property setter:  <property>nullable</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setNullable(Boolean value) {
		nullable = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setMaxProperties(Integer value) {
		maxProperties = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setMinProperties(Integer value) {
		minProperties = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<String> getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"http"</js>
	 * 		<li><js>"https"</js>
	 * 		<li><js>"ws"</js>
	 * 		<li><js>"wss"</js>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setRequired(Collection<String> value) {
		required = listFrom(value);
		return this;
	}

	/**
	 * Same as {@link #addRequired(String...)}.
	 *
	 * @param values
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Collection&lt;String&gt;</code>
	 * 		<li><code>String</code> - JSON array representation of <code>Collection&lt;String&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	schemes(<js>"['scheme1','scheme2']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	schemes(<js>"scheme1</js>, <js>"scheme2"</js>);
	 * 			</p>
	 * 	</ul>
	 * @return This object
	 */
	public SchemaInfo addRequired(String...values) {
		required = listBuilder(String.class).sparse().add(values).build();
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
	public SchemaInfo setEnum(Collection<Object> value) {
		_enum = listFrom(value);
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
	public SchemaInfo addEnum(Object...values) {
		setEnum(setBuilder(Object.class).sparse().addAny(values).build());
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
	 * 	<br>Possible values include:
	 * 	<ul>
	 * 		<li><js>"object"</js>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 		<li><js>"file"</js>
	 * 	</ul>
	 * @return This object
	 */
	public SchemaInfo setType(String value) {
		type = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setItems(Items value) {
		items = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setAllOf(Collection<Object> value) {
		allOf = listFrom(value);
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
	 * @return This object
	 */
	public SchemaInfo addAllOf(Object...values) {
		allOf = listBuilder(allOf).sparse().addAny(values).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getAnyOf() {
		return anyOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setAnyOf(Collection<Object> value) {
		anyOf = listFrom(value);
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
	 * @return This object
	 */
	public SchemaInfo addAnyOf(Object...values) {
		anyOf = listBuilder(anyOf).sparse().addAny(values).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getOneOf() {
		return oneOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setOneOf(Collection<Object> value) {
		oneOf = listFrom(value);
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
	 * @return This object
	 */
	public SchemaInfo addOneOf(Object...values) {
		oneOf = listBuilder(oneOf).sparse().addAny(values).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, SchemaInfo> getProperties() {
		return properties;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setProperties(Map<String, SchemaInfo> value) {
		properties = copyOf(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getAdditionalProperties() {
		return additionalProperties;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setAdditionalProperties(SchemaInfo value) {
		additionalProperties = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>not</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getNot() {
		return not;
	}

	/**
	 * Bean property setter:  <property>not</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setNot(SchemaInfo value) {
		not = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>discriminator</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Discriminator getDiscriminator() {
		return discriminator;
	}

	/**
	 * Bean property setter:  <property>discriminator</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setDiscriminator(Discriminator value) {
		discriminator = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setReadOnly(Boolean value) {
		readOnly = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>WriteOnly</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getWriteOnly() {
		return writeOnly;
	}

	/**
	 * Bean property setter:  <property>WriteOnly</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setWriteOnly(Boolean value) {
		writeOnly = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() {
		return deprecated;
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setDeprecated(Boolean value) {
		deprecated = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setXml(Xml value) {
		xml = value;
		return this;
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
	 * @return This object
	 */
	public SchemaInfo setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
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
	 * @return This object
	 */
	public SchemaInfo setExample(Object value) {
		example = value;
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
	public SchemaInfo setRef(Object value) {
		ref = stringify(value);
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

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
			case "oneOf": return toType(getOneOf(), type);
			case "anyOf": return toType(getAnyOf(), type);
			case "properties": return toType(getProperties(), type);
			case "additionalProperties": return toType(getAdditionalProperties(), type);
			case "not": return toType(getNot(), type);
			case "nullable": return toType(getNullable(), type);
			case "deprecated": return toType(getDeprecated(), type);
			case "discriminator": return toType(getDiscriminator(), type);
			case "readOnly": return toType(getReadOnly(), type);
			case "writeOnly": return toType(getWriteOnly(), type);
			case "xml": return toType(getXml(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "example": return toType(getExample(), type);
			case "$ref": return toType(getRef(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SchemaInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "format": return setFormat(stringify(value));
			case "title": return setTitle(stringify(value));
			case "description": return setDescription(stringify(value));
			case "default": return setDefault(value);
			case "multipleOf": return setMultipleOf(toNumber(value));
			case "maximum": return setMaximum(toNumber(value));
			case "exclusiveMaximum": return setExclusiveMaximum(toBoolean(value));
			case "minimum": return setMinimum(toNumber(value));
			case "exclusiveMinimum": return setExclusiveMinimum(toBoolean(value));
			case "maxLength": return setMaxLength(toInteger(value));
			case "minLength": return setMinLength(toInteger(value));
			case "pattern": return setPattern(stringify(value));
			case "maxItems": return setMaxItems(toInteger(value));
			case "minItems": return setMinItems(toInteger(value));
			case "uniqueItems": return setUniqueItems(toBoolean(value));
			case "maxProperties": return setMaxProperties(toInteger(value));
			case "minProperties": return setMinProperties(toInteger(value));
			case "required": return addRequired(stringify(value));
			case "enum": return addEnum(value);
			case "type": return setType(stringify(value));
			case "items": return setItems(toType(value, Items.class));
			case "allOf": return addAllOf(value);
			case "anyOf": return addAnyOf(value);
			case "oneOf": return addOneOf(value);
			case "properties": return setProperties(mapBuilder(String.class,SchemaInfo.class).sparse().addAny(value).build());
			case "additionalProperties": return setAdditionalProperties(toType(value, SchemaInfo.class));
			case "not": return setNot(toType(value, SchemaInfo.class));
			case "nullable": return setNullable(toBoolean(value));
			case "deprecated": return setDeprecated(toBoolean(value));
			case "discriminator": return setDiscriminator(toType(value, Discriminator.class));
			case "readOnly": return setReadOnly(toBoolean(value));
			case "writeOnly": return setWriteOnly(toBoolean(value));
			case "xml": return setXml(toType(value, Xml.class));
			case "externalDocs": return setExternalDocs(toType(value, ExternalDocumentation.class));
			case "example": return setExample(value);
			case "$ref": return setRef(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(format != null, "format")
			.addIf(title != null, "title")
			.addIf(description != null, "description")
			.addIf(_default != null, "default")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(maximum != null, "maximum")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(minimum != null, "minimum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(maxLength != null, "maxLength")
			.addIf(minLength != null, "minLength")
			.addIf(pattern != null, "pattern")
			.addIf(maxItems != null, "maxItems")
			.addIf(minItems != null, "minItems")
			.addIf(uniqueItems != null, "uniqueItems")
			.addIf(maxProperties != null, "maxProperties")
			.addIf(minProperties != null, "minProperties")
			.addIf(required != null, "required")
			.addIf(_enum != null, "enum")
			.addIf(type != null, "type")
			.addIf(items != null, "items")
			.addIf(allOf != null, "allOf")
			.addIf(anyOf != null, "anyOf")
			.addIf(oneOf != null, "oneOf")
			.addIf(properties != null, "properties")
			.addIf(additionalProperties != null, "additionalProperties")
			.addIf(nullable != null, "nullable")
			.addIf(deprecated != null, "deprecated")
			.addIf(not != null, "not")
			.addIf(discriminator != null, "discriminator")
			.addIf(readOnly != null, "readOnly")
			.addIf(writeOnly != null, "writeOnly")
			.addIf(xml != null, "xml")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(example != null, "example")
			.addIf(ref != null, "$ref")
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
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public SchemaInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (ref != null) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			SchemaInfo r = swagger.findRef(ref, SchemaInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		if (properties != null)
			for (Map.Entry<String, SchemaInfo> e : properties.entrySet())
				e.setValue(e.getValue().resolveRefs(swagger, refStack, maxDepth));

		if (additionalProperties != null)
			additionalProperties = additionalProperties.resolveRefs(swagger, refStack, maxDepth);

		this.example = null;

		return this;
	}
}
