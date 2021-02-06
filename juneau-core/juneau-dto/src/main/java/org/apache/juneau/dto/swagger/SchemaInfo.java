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

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
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
 * <p class='bcode w800'>
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
 * <p class='bcode w800'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"type"</js>: <js>"string"</js>,
 * 		<js>"title"</js>: <js>"foo"</js>
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoSwagger}
 * </ul>
 */
@Bean(properties="format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,enum,type,items,allOf,properties,additionalProperties,discriminator,readOnly,xml,externalDocs,example,$ref,*")
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
		readOnly;
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
		required;
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
		this.allOf = newSet(copyFrom.allOf);
		this._default = copyFrom._default;
		this.description = copyFrom.description;
		this.discriminator = copyFrom.discriminator;
		this._enum = newSet(copyFrom._enum);
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
		this.required = newSet(copyFrom.required);
		this.title = copyFrom.title;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
		this.xml = copyFrom.xml == null ? null : copyFrom.xml.copy();

		if (copyFrom.properties == null) {
			this.properties = null;
		} else {
			this.properties = new LinkedHashMap<>();
			for (Map.Entry<String,SchemaInfo> e : copyFrom.properties.entrySet())
				this.properties.put(e.getKey(), e.getValue().copy());
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
	// additionalProperties
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
	 */
	public void setAdditionalProperties(SchemaInfo value) {
		additionalProperties = value;
	}

	/**
	 * Bean property fluent getter:  <property>additionalProperties</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<SchemaInfo> additionalProperties() {
		return Optional.ofNullable(getAdditionalProperties());
	}

	/**
	 * Bean property fluent setter:  <property>additionalProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo additionalProperties(SchemaInfo value) {
		setAdditionalProperties(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>additionalProperties</property>.
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo additionalProperties(String json) {
		setAdditionalProperties(toType(json, SchemaInfo.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// allOf
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setAllOf(Collection<Object> value) {
		allOf = newSet(value);
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addAllOf(Collection<Object> values) {
		allOf = setBuilder(allOf).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>allOf</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<Object>> allOf() {
		return Optional.ofNullable(getAllOf());
	}

	/**
	 * Bean property fluent setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The values to set on this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo allOf(Collection<Object> value) {
		setAllOf(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can contains JSON arrays.
	 * 	<br>Valid types:
	 * @return This object (for method chaining).
	 */
	public SchemaInfo allOf(Object...value) {
		setAllOf(setBuilder(Object.class).sparse().addAny(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// default
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setDefault(Object value) {
		_default = value;
	}

	/**
	 * Bean property fluent getter:  <property>default</property>.
	 *
	 * <p>
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
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
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _default(Object value) {
		setDefault(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// description
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> description() {
		return Optional.ofNullable(getDescription());
	}

	/**
	 * Bean property fluent setter:  <property>description</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// discriminator
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setDiscriminator(String value) {
		discriminator = value;
	}

	/**
	 * Bean property fluent getter:  <property>discriminator</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> discriminator() {
		return Optional.ofNullable(getDiscriminator());
	}

	/**
	 * Bean property fluent setter:  <property>discriminator</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo discriminator(String value) {
		setDiscriminator(value);
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
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addEnum(Collection<Object> value) {
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
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _enum(Object...value) {
		setEnum(setBuilder(Object.class).sparse().addAny(value).build());
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _enum(Collection<Object> value) {
		setEnum(value);
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
	public SchemaInfo example(Object value) {
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
	public SchemaInfo exclusiveMaximum(Boolean value) {
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
	public SchemaInfo exclusiveMaximum(String value) {
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
	public SchemaInfo exclusiveMinimum(Boolean value) {
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
	public SchemaInfo exclusiveMinimum(String value) {
		setExclusiveMinimum(toBoolean(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// externalDocs
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExternalDocs(ExternalDocumentation value) {
		externalDocs = value;
	}

	/**
	 * Bean property fluent getter:  <property>externalDocs</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ExternalDocumentation> externalDocs() {
		return Optional.ofNullable(getExternalDocs());
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo externalDocs(ExternalDocumentation value) {
		setExternalDocs(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>externalDocs</property>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	externalDocs(<js>"{description:'description',url:'url'}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo externalDocs(String json) {
		setExternalDocs(toType(json, ExternalDocumentation.class));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// format
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setFormat(String value) {
		format = value;
	}

	/**
	 * Bean property fluent getter:  <property>format</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> format() {
		return Optional.ofNullable(getFormat());
	}

	/**
	 * Bean property fluent setter:  <property>format</property>.
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo format(String value) {
		setFormat(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// items
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setItems(Items value) {
		items = value;
	}

	/**
	 * Bean property fluent getter:  <property>items</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Items> items() {
		return Optional.ofNullable(getItems());
	}

	/**
	 * Bean property fluent setter:  <property>items</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo items(Items value) {
		setItems(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>items</property>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	items(<js>"{type:'type',format:'format',...}"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo items(String value) {
		setItems(toType(value, Items.class));
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
	public SchemaInfo maximum(Number value) {
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
	public SchemaInfo maximum(String value) {
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
	public SchemaInfo maxItems(Integer value) {
		setMaxItems(value);
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
	public SchemaInfo maxItems(String value) {
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
	public SchemaInfo maxLength(Integer value) {
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
	public SchemaInfo maxLength(String value) {
		setMaxLength(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// maxProperties
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMaxProperties(Integer value) {
		maxProperties = value;
	}

	/**
	 * Bean property fluent getter:  <property>maxProperties</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> maxProperties() {
		return Optional.ofNullable(getMaxProperties());
	}

	/**
	 * Bean property fluent setter:  <property>maxProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxProperties(Integer value) {
		setMaxProperties(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>maxProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxProperties(String value) {
		setMaxProperties(toInteger(value));
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
	public SchemaInfo minimum(Number value) {
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
	public SchemaInfo minimum(String value) {
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
	public SchemaInfo minItems(Integer value) {
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
	public SchemaInfo minItems(String value) {
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
	public SchemaInfo minLength(Integer value) {
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
	public SchemaInfo minLength(String value) {
		setMinLength(toInteger(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// minProperties
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setMinProperties(Integer value) {
		minProperties = value;
	}

	/**
	 * Bean property fluent getter:  <property>minProperties</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Integer> minProperties() {
		return Optional.ofNullable(getMinProperties());
	}

	/**
	 * Bean property fluent setter:  <property>minProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minProperties(Integer value) {
		setMinProperties(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>minProperties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minProperties(String value) {
		setMinProperties(toInteger(value));
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
	public SchemaInfo multipleOf(Number value) {
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
	public SchemaInfo multipleOf(String value) {
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo pattern(String value) {
		setPattern(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// properties
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setProperties(Map<String,SchemaInfo> value) {
		properties = newMap(value);
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addProperties(Map<String,SchemaInfo> values) {
		properties = mapBuilder(properties).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>properties</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,SchemaInfo>> properties() {
		return Optional.ofNullable(getProperties());
	}

	/**
	 * Bean property fluent setter:  <property>properties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo properties(Map<String,SchemaInfo> value) {
		setProperties(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>properties</property>.
	 *
	 * @param json
	 * 	The value to set on this property as a JSON object.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo properties(String json) {
		setProperties(mapBuilder(String.class,SchemaInfo.class).sparse().addJson(json).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// readOnly
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setReadOnly(Boolean value) {
		readOnly = value;
	}

	/**
	 * Bean property fluent getter:  <property>readOnly</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Boolean> readOnly() {
		return Optional.ofNullable(getReadOnly());
	}

	/**
	 * Bean property fluent  setter:  <property>readOnly</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo readOnly(Boolean value) {
		setReadOnly(value);
		return this;
	}

	/**
	 * Bean property fluent  setter:  <property>readOnly</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo readOnly(String value) {
		setReadOnly(toBoolean(value));
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
	public SchemaInfo ref(String value) {
		setRef(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// required
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getRequired() {
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
	 */
	public void setRequired(Collection<String> value) {
		required = newSet(value);
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addRequired(Collection<String> value) {
		required = setBuilder(required).sparse().addAny(value).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>required</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Set<String>> required() {
		return Optional.ofNullable(getRequired());
	}

	/**
	 * Bean property fluent setter:  <property>required</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo required(Collection<String> value) {
		setRequired(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>required</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo required(String...value) {
		setRequired(setBuilder(String.class).sparse().addJson(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// title
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setTitle(String value) {
		title = value;
	}

	/**
	 * Bean property fluent getter:  <property>title</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> title() {
		return Optional.ofNullable(getTitle());
	}

	/**
	 * Bean property fluent setter:  <property>title</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo title(String value) {
		setTitle(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setType(String value) {
		type = value;
	}

	/**
	 * Bean property fluent getter:  <property>type</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> type() {
		return Optional.ofNullable(getType());
	}

	/**
	 * Bean property fluent setter:  <property>type</property>.
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
	 * @return This object (for method chaining).
	 */
	public SchemaInfo type(String value) {
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
	public SchemaInfo uniqueItems(Boolean value) {
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
	public SchemaInfo uniqueItems(String value) {
		setUniqueItems(toBoolean(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// xml
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setXml(Xml value) {
		xml = value;
	}

	/**
	 * Bean property fluent getter:  <property>xml</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Xml> xml() {
		return Optional.ofNullable(getXml());
	}

	/**
	 * Bean property fluent setter:  <property>xml</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo xml(Xml value) {
		setXml(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>xml</property>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	xml(<js>"{name:'name',namespace:'namespace',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo xml(String json) {
		setXml(toType(json, Xml.class));
		return this;
	}


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
			case "required": return toType(getRequired(), type);
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
			case "additionalProperties": return additionalProperties(toType(value, SchemaInfo.class));
			case "allOf": return allOf(value);
			case "default": return _default(value);
			case "description": return description(stringify(value));
			case "discriminator": return discriminator(stringify(value));
			case "enum": return _enum(value);
			case "example": return example(value);
			case "exclusiveMaximum": return exclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum": return exclusiveMinimum(toBoolean(value));
			case "externalDocs": return externalDocs(toType(value, ExternalDocumentation.class));
			case "format": return format(stringify(value));
			case "items": return items(toType(value, Items.class));
			case "maximum": return maximum(toNumber(value));
			case "maxItems": return maxItems(toInteger(value));
			case "maxLength": return maxLength(toInteger(value));
			case "maxProperties": return maxProperties(toInteger(value));
			case "minimum": return minimum(toNumber(value));
			case "minItems": return minItems(toInteger(value));
			case "minLength": return minLength(toInteger(value));
			case "minProperties": return minProperties(toInteger(value));
			case "multipleOf": return multipleOf(toNumber(value));
			case "pattern": return pattern(stringify(value));
			case "properties": return properties(mapBuilder(String.class,SchemaInfo.class).sparse().addAny(value).build());
			case "readOnly": return readOnly(toBoolean(value));
			case "$ref": return ref(stringify(value));
			case "required": return required(listBuilder(String.class).sparse().addAny(value).build());
			case "title": return title(stringify(value));
			case "type": return type(stringify(value));
			case "uniqueItems": return uniqueItems(toBoolean(value));
			case "xml": return xml(toType(value, Xml.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(additionalProperties != null, "additionalProperties")
			.appendIf(allOf != null, "allOf")
			.appendIf(_default != null, "default")
			.appendIf(description != null, "description")
			.appendIf(discriminator != null, "discriminator")
			.appendIf(_enum != null, "enum")
			.appendIf(example != null, "example")
			.appendIf(exclusiveMaximum != null, "exclusiveMaximum")
			.appendIf(exclusiveMinimum != null, "exclusiveMinimum")
			.appendIf(externalDocs != null, "externalDocs")
			.appendIf(format != null, "format")
			.appendIf(items != null, "items")
			.appendIf(maximum != null, "maximum")
			.appendIf(maxItems != null, "maxItems")
			.appendIf(maxLength != null, "maxLength")
			.appendIf(maxProperties != null, "maxProperties")
			.appendIf(minimum != null, "minimum")
			.appendIf(minItems != null, "minItems")
			.appendIf(minLength != null, "minLength")
			.appendIf(minProperties != null, "minProperties")
			.appendIf(multipleOf != null, "multipleOf")
			.appendIf(pattern != null, "pattern")
			.appendIf(properties != null, "properties")
			.appendIf(readOnly != null, "readOnly")
			.appendIf(ref != null, "$ref")
			.appendIf(required != null, "required")
			.appendIf(title != null, "title")
			.appendIf(type != null, "type")
			.appendIf(uniqueItems != null, "uniqueItems")
			.appendIf(xml != null, "xml");
		return new MultiSet<>(s, super.keySet());
	}



	/**
	 * Returns <jk>true</jk> if this schema info has one or more properties defined on it.
	 *
	 * @return <jk>true</jk> if this schema info has one or more properties defined on it.
	 */
	public boolean hasProperties() {
		return properties != null && ! properties.isEmpty();
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
			for (Map.Entry<String,SchemaInfo> e : properties.entrySet())
				e.setValue(e.getValue().resolveRefs(swagger, refStack, maxDepth));

		if (additionalProperties != null)
			additionalProperties = additionalProperties.resolveRefs(swagger, refStack, maxDepth);

		this.example = null;

		return this;
	}
}
