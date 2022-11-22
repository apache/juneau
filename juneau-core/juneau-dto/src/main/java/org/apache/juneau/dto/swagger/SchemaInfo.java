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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	SchemaInfo <jv>info</jv> = <jsm>schemaInfo</jsm>()
 * 		.type(<js>"string"</js>)
 * 		.title(<js>"foo"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>info</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>info</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"type"</js>: <js>"string"</js>,
 * 		<js>"title"</js>: <js>"foo"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,requiredProperties,enum,type,items,allOf,properties,additionalProperties,discriminator,readOnly,xml,externalDocs,example,$ref,*")
@FluentSetters
public class SchemaInfo extends SwaggerElement {

	private String
		format,
		title,
		description,
		pattern,
		type,
		discriminator,
		ref;
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
		readOnly,
		required;
	private Object
		_default,
		example;
	private Items items;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private Set<Object>
		_enum,
		allOf;
	private Set<String>
		requiredProperties;
	private Map<String,SchemaInfo> properties;
	private SchemaInfo additionalProperties;

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

		this.additionalProperties = copyFrom.additionalProperties == null ? null : copyFrom.additionalProperties.copy();
		this.allOf = copyOf(copyFrom.allOf);
		this._default = copyFrom._default;
		this.description = copyFrom.description;
		this.discriminator = copyFrom.discriminator;
		this._enum = copyOf(copyFrom._enum);
		this.example = copyFrom.example;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		this.format = copyFrom.format;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.maximum = copyFrom.maximum;
		this.maxItems = copyFrom.maxItems;
		this.maxLength = copyFrom.maxLength;
		this.maxProperties = copyFrom.maxProperties;
		this.minimum = copyFrom.minimum;
		this.minItems = copyFrom.minItems;
		this.minLength = copyFrom.minLength;
		this.minProperties = copyFrom.minProperties;
		this.multipleOf = copyFrom.multipleOf;
		this.pattern = copyFrom.pattern;
		this.readOnly = copyFrom.readOnly;
		this.ref = copyFrom.ref;
		this.required = copyFrom.required;
		this.requiredProperties = copyOf(copyFrom.requiredProperties);
		this.title = copyFrom.title;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
		this.xml = copyFrom.xml == null ? null : copyFrom.xml.copy();

		if (copyFrom.properties == null) {
			this.properties = null;
		} else {
			this.properties = map();
			copyFrom.properties.forEach((k,v) -> this.properties.put(k, v.copy()));
		}
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SchemaInfo copy() {
		return new SchemaInfo(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	 * @return This object.
	 */
	public SchemaInfo setAdditionalProperties(SchemaInfo value) {
		additionalProperties = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getAllOf() {
		return allOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setAllOf(Collection<Object> value) {
		allOf = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addAllOf(Object...values) {
		allOf = setBuilder(allOf).sparse().add(values).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can contains JSON arrays.
	 * 	<br>Valid types:
	 * @return This object.
	 */
	public SchemaInfo setAllOf(Object...value) {
		setAllOf(setBuilder(Object.class).sparse().addAny(value).build());
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
	 * @return This object.
	 */
	public SchemaInfo setDefault(Object value) {
		_default = value;
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
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setDescription(String value) {
		description = value;
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
	 * @return This object.
	 */
	public SchemaInfo setDiscriminator(String value) {
		discriminator = value;
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
	public SchemaInfo setEnum(Collection<Object> value) {
		_enum = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addEnum(Object...value) {
		_enum = setBuilder(_enum).sparse().add(value).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public SchemaInfo setEnum(Object...value) {
		setEnum(setBuilder(Object.class).sparse().addAny(value).build());
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
	public SchemaInfo setExample(Object value) {
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
	public SchemaInfo setExclusiveMaximum(Boolean value) {
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
	public SchemaInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
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
	 * @return This object.
	 */
	public SchemaInfo setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
		return this;
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
	 * @return This object.
	 */
	public SchemaInfo setFormat(String value) {
		format = value;
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
	 * @return This object.
	 */
	public SchemaInfo setItems(Items value) {
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
	public SchemaInfo setMaximum(Number value) {
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
	public SchemaInfo setMaxItems(Integer value) {
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
	public SchemaInfo setMaxLength(Integer value) {
		maxLength = value;
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
	 * @return This object.
	 */
	public SchemaInfo setMaxProperties(Integer value) {
		maxProperties = value;
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
	public SchemaInfo setMinimum(Number value) {
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
	public SchemaInfo setMinItems(Integer value) {
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
	public SchemaInfo setMinLength(Integer value) {
		minLength = value;
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
	 * @return This object.
	 */
	public SchemaInfo setMinProperties(Integer value) {
		minProperties = value;
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
	public SchemaInfo setMultipleOf(Number value) {
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
	public SchemaInfo setPattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SchemaInfo> getProperties() {
		return properties;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setProperties(Map<String,SchemaInfo> value) {
		properties = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * @param key The property key.
	 * @param value The property value.
	 * @return This object.
	 */
	public SchemaInfo addProperty(String key, SchemaInfo value) {
		properties = mapBuilder(properties).sparse().add(key, value).build();
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
	 * @return This object.
	 */
	public SchemaInfo setReadOnly(Boolean value) {
		readOnly = value;
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
	public SchemaInfo setRef(String value) {
		ref = value;
		return this;
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
	 * @return This object.
	 */
	public SchemaInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>requiredProperties</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getRequiredProperties() {
		return requiredProperties;
	}

	/**
	 * Bean property setter:  <property>requiredProperties</property>.
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
	 * @return This object.
	 */
	public SchemaInfo setRequiredProperties(Collection<String> value) {
		requiredProperties = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>requiredProperties</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addRequiredProperties(String...value) {
		requiredProperties = setBuilder(requiredProperties).sparse().add(value).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>requiredProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public SchemaInfo setRequiredProperties(String...value) {
		setRequiredProperties(setBuilder(String.class).sparse().addJson(value).build());
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
	 * @return This object.
	 */
	public SchemaInfo setTitle(String value) {
		title = value;
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
	 * @return This object.
	 */
	public SchemaInfo setType(String value) {
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
	public SchemaInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
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
	 * @return This object.
	 */
	public SchemaInfo setXml(Xml value) {
		xml = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "additionalProperties": return toType(getAdditionalProperties(), type);
			case "allOf": return toType(getAllOf(), type);
			case "default": return toType(getDefault(), type);
			case "description": return toType(getDescription(), type);
			case "discriminator": return toType(getDiscriminator(), type);
			case "enum": return toType(getEnum(), type);
			case "example": return toType(getExample(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "externalDocs": return toType(getExternalDocs(), type);
			case "format": return toType(getFormat(), type);
			case "items": return toType(getItems(), type);
			case "maximum": return toType(getMaximum(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "maxProperties": return toType(getMaxProperties(), type);
			case "minimum": return toType(getMinimum(), type);
			case "minItems": return toType(getMinItems(), type);
			case "minLength": return toType(getMinLength(), type);
			case "minProperties": return toType(getMinProperties(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "pattern": return toType(getPattern(), type);
			case "properties": return toType(getProperties(), type);
			case "readOnly": return toType(getReadOnly(), type);
			case "$ref": return toType(getRef(), type);
			case "rquired": return toType(getRequired(), type);
			case "requiredProperties": return toType(getRequiredProperties(), type);
			case "title": return toType(getTitle(), type);
			case "type": return toType(getType(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
			case "xml": return toType(getXml(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public SchemaInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "additionalProperties": return setAdditionalProperties(toType(value, SchemaInfo.class));
			case "allOf": return setAllOf(value);
			case "default": return setDefault(value);
			case "description": return setDescription(stringify(value));
			case "discriminator": return setDiscriminator(stringify(value));
			case "enum": return setEnum(value);
			case "example": return setExample(value);
			case "exclusiveMaximum": return setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum": return setExclusiveMinimum(toBoolean(value));
			case "externalDocs": return setExternalDocs(toType(value, ExternalDocumentation.class));
			case "format": return setFormat(stringify(value));
			case "items": return setItems(toType(value, Items.class));
			case "maximum": return setMaximum(toNumber(value));
			case "maxItems": return setMaxItems(toInteger(value));
			case "maxLength": return setMaxLength(toInteger(value));
			case "maxProperties": return setMaxProperties(toInteger(value));
			case "minimum": return setMinimum(toNumber(value));
			case "minItems": return setMinItems(toInteger(value));
			case "minLength": return setMinLength(toInteger(value));
			case "minProperties": return setMinProperties(toInteger(value));
			case "multipleOf": return setMultipleOf(toNumber(value));
			case "pattern": return setPattern(stringify(value));
			case "properties": return setProperties(mapBuilder(String.class,SchemaInfo.class).sparse().addAny(value).build());
			case "readOnly": return setReadOnly(toBoolean(value));
			case "$ref": return setRef(stringify(value));
			case "required": return setRequired(toBoolean(value));
			case "requiredProperties": return setRequiredProperties(listBuilder(String.class).sparse().addAny(value).build());
			case "title": return setTitle(stringify(value));
			case "type": return setType(stringify(value));
			case "uniqueItems": return setUniqueItems(toBoolean(value));
			case "xml": return setXml(toType(value, Xml.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(additionalProperties != null, "additionalProperties")
			.addIf(allOf != null, "allOf")
			.addIf(_default != null, "default")
			.addIf(description != null, "description")
			.addIf(discriminator != null, "discriminator")
			.addIf(_enum != null, "enum")
			.addIf(example != null, "example")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(externalDocs != null, "externalDocs")
			.addIf(format != null, "format")
			.addIf(items != null, "items")
			.addIf(maximum != null, "maximum")
			.addIf(maxItems != null, "maxItems")
			.addIf(maxLength != null, "maxLength")
			.addIf(maxProperties != null, "maxProperties")
			.addIf(minimum != null, "minimum")
			.addIf(minItems != null, "minItems")
			.addIf(minLength != null, "minLength")
			.addIf(minProperties != null, "minProperties")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(pattern != null, "pattern")
			.addIf(properties != null, "properties")
			.addIf(readOnly != null, "readOnly")
			.addIf(ref != null, "$ref")
			.addIf(required != null, "required")
			.addIf(requiredProperties != null, "requiredProperties")
			.addIf(title != null, "title")
			.addIf(type != null, "type")
			.addIf(uniqueItems != null, "uniqueItems")
			.addIf(xml != null, "xml")
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
			properties.entrySet().forEach(x -> x.setValue(x.getValue().resolveRefs(swagger, refStack, maxDepth)));

		if (additionalProperties != null)
			additionalProperties = additionalProperties.resolveRefs(swagger, refStack, maxDepth);

		this.example = null;

		return this;
	}
}
