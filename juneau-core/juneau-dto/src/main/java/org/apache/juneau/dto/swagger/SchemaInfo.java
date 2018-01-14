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
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Swagger'>Swagger</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a>
 * 	</li>
 * </ul>
 */
@Bean(properties="format,title,description,default,multipleOf,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,maxProperties,minProperties,required,enum,type,items,allOf,properties,additionalProperties,discriminator,readOnly,xml,externalDocs,example")
@SuppressWarnings({ "unchecked" })
public class SchemaInfo extends SwaggerElement {

	private String format;
	private String title;
	private String description;
	private Object _default;
	private Number multipleOf;
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
	private Integer maxProperties;
	private Integer minProperties;
	private Boolean required;
	private List<Object> _enum;
	private String type;
	private Items items;
	private List<Object> allOf;
	private Map<String,Map<String,Object>> properties;
	private Map<String,Object> additionalProperties;
	private String discriminator;
	private Boolean readOnly;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private Object example;

	/**
	 * Bean property getter:  <property>format</property>.
	 * 
	 * <p>
	 * See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further
	 * details.
	 * 
	 * @return The value of the <property>format</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 * 
	 * <p>
	 * See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further
	 * details.
	 * 
	 * @param format The new value for the <property>format</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setFormat(String format) {
		this.format = format;
		return this;
	}

	/**
	 * Synonym for {@link #setFormat(String)}.
	 * 
	 * @param format The new value for the <property>format</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo format(String format) {
		return setFormat(format);
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 * 
	 * @return The value of the <property>title</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 * 
	 * @param title The new value for the <property>title</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Synonym for {@link #setTitle(String)}.
	 * 
	 * @param title The new value for the <property>title</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo title(String title) {
		return setTitle(title);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * 
	 * <p>
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used
	 * for rich text representation.
	 * 
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * 
	 * <p>
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used
	 * for rich text representation.
	 * 
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Synonym for {@link #setDescription(String)}.
	 * 
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo description(String description) {
		return setDescription(description);
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 * 
	 * <p>
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
	 * 
	 * @return The value of the <property>default</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * @param _default The new value for the <property>default</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDefault(Object _default) {
		this._default = _default;
		return this;
	}

	/**
	 * Synonym for {@link #setDefault(Object)}.
	 * 
	 * @param _default The new value for the <property>default</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _default(Object _default) {
		return setDefault(_default);
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 * 
	 * @return The value of the <property>multipleOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 * 
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
		return this;
	}

	/**
	 * Synonym for {@link #setMultipleOf(Number)}.
	 * 
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo multipleOf(Number multipleOf) {
		return setMultipleOf(multipleOf);
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 * 
	 * @return The value of the <property>maximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 * 
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaximum(Number maximum) {
		this.maximum = maximum;
		return this;
	}

	/**
	 * Synonym for {@link #setMaximum(Number)}.
	 * 
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maximum(Number maximum) {
		return setMaximum(maximum);
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 * 
	 * @return
	 * 	The value of the <property>exclusiveMaximum</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 * 
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	/**
	 * Synonym for {@link #setExclusiveMaximum(Boolean)}.
	 * 
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo exclusiveMaximum(Boolean exclusiveMaximum) {
		return setExclusiveMaximum(exclusiveMaximum);
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 * 
	 * @return The value of the <property>minimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 * 
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinimum(Number minimum) {
		this.minimum = minimum;
		return this;
	}

	/**
	 * Synonym for {@link #setMinimum(Number)}.
	 * 
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minimum(Number minimum) {
		return setMinimum(minimum);
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 * 
	 * @return
	 * 	The value of the <property>exclusiveMinimum</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 * 
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	/**
	 * Synonym for {@link #setExclusiveMinimum(Boolean)}.
	 * 
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo exclusiveMinimum(Boolean exclusiveMinimum) {
		return setExclusiveMinimum(exclusiveMinimum);
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 * 
	 * @return The value of the <property>maxLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 * 
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	/**
	 * Synonym for {@link #setMaxLength(Integer)}.
	 * 
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxLength(Integer maxLength) {
		return setMaxLength(maxLength);
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 * 
	 * @return The value of the <property>minLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 * 
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinLength(Integer minLength) {
		this.minLength = minLength;
		return this;
	}

	/**
	 * Synonym for {@link #setMinLength(Integer)}.
	 * 
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minLength(Integer minLength) {
		return setMinLength(minLength);
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 * 
	 * @return The value of the <property>pattern</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 * 
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Synonym for {@link #setPattern(String)}.
	 * 
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo pattern(String pattern) {
		return setPattern(pattern);
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 * 
	 * @return The value of the <property>maxItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 * 
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * Synonym for {@link #setMaxItems(Integer)}.
	 * 
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxItems(Integer maxItems) {
		return setMaxItems(maxItems);
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 * 
	 * @return The value of the <property>minItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 * 
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinItems(Integer minItems) {
		this.minItems = minItems;
		return this;
	}

	/**
	 * Synonym for {@link #setMinItems(Integer)}.
	 * 
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minItems(Integer minItems) {
		return setMinItems(minItems);
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 * 
	 * @return The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not
	 * set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}
	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 * 
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
		return this;
	}

	/**
	 * Synonym for {@link #setUniqueItems(Boolean)}.
	 * 
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo uniqueItems(Boolean uniqueItems) {
		return setUniqueItems(uniqueItems);
	}

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 * 
	 * @return The value of the <property>maxProperties</property> property on this bean, or <jk>null</jk> if it is
	 * not set.
	 */
	public Integer getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Bean property setter:  <property>maxProperties</property>.
	 * 
	 * @param maxProperties The new value for the <property>maxProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMaxProperties(Integer maxProperties) {
		this.maxProperties = maxProperties;
		return this;
	}

	/**
	 * Synonym for {@link #setMaxProperties(Integer)}.
	 * 
	 * @param maxProperties The new value for the <property>maxProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo maxProperties(Integer maxProperties) {
		return setMaxProperties(maxProperties);
	}

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 * 
	 * @return The value of the <property>minProperties</property> property on this bean, or <jk>null</jk> if it is
	 * not set.
	 */
	public Integer getMinProperties() {
		return minProperties;
	}

	/**
	 * Bean property setter:  <property>minProperties</property>.
	 * 
	 * @param minProperties The new value for the <property>minProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setMinProperties(Integer minProperties) {
		this.minProperties = minProperties;
		return this;
	}

	/**
	 * Synonym for {@link #setMinProperties(Integer)}.
	 * 
	 * @param minProperties The new value for the <property>minProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo minProperties(Integer minProperties) {
		return setMinProperties(minProperties);
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 * 
	 * @return The value of the <property>required</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 * 
	 * @param required The new value for the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setRequired(Boolean required) {
		this.required = required;
		return this;
	}

	/**
	 * Synonym for {@link #setRequired(Boolean)}.
	 * 
	 * @param required The new value for the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo required(Boolean required) {
		return setRequired(required);
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 * 
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 * 
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setEnum(List<Object> _enum) {
		this._enum = _enum;
		return this;
	}

	/**
	 * Bean property adder:  <property>enum</property>.
	 * 
	 * @param _enum The new values to add to the <property>enum</property> property on this bean.
	 * These can either be individual objects or {@link Collection Collections} of objects.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addEnum(Object..._enum) {
		for (Object o  : _enum) {
			if (o != null) {
				if (o instanceof Collection)
					addEnum((Collection<Object>)o);
				else {
					if (this._enum == null)
						this._enum = new LinkedList<>();
					this._enum.add(o);
				}
			}
		}
		return this;
	}

	/**
	 * Synonym for {@link #addEnum(Object...)}.
	 * 
	 * @param _enum
	 * 	The new values to add to the <property>enum</property> property on this bean.
	 * 	These can either be individual objects or {@link Collection Collections} of objects.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo _enum(Object..._enum) {
		return addEnum(_enum);
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * 
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * 
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Synonym for {@link #setType(String)}.
	 * 
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo type(String type) {
		return setType(type);
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * 
	 * @return The value of the <property>items</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 * 
	 * @param items The new value for the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setItems(Items items) {
		this.items = items;
		return this;
	}

	/**
	 * Synonym for {@link #setItems(Items)}.
	 * 
	 * @param items The new value for the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo items(Items items) {
		return setItems(items);
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 * 
	 * @return The value of the <property>allOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getAllOf() {
		return allOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 * 
	 * @param allOf The new value for the <property>allOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setAllOf(List<Object> allOf) {
		this.allOf = allOf;
		return this;
	}

	/**
	 * Bean property adder:  <property>enum</property>.
	 * 
	 * @param allOf
	 * 	The new values to add to the <property>allOf</property> property on this bean.
	 * 	These can either be individual objects or {@link Collection Collections} of objects.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addAllOf(Object...allOf) {
		for (Object o  : allOf) {
			if (o != null) {
				if (o instanceof Collection)
					addAllOf((Collection<Object>)o);
				else {
					if (this.allOf == null)
						this.allOf = new LinkedList<>();
					this.allOf.add(o);
				}
			}
		}
		return this;
	}

	/**
	 * Synonym for {@link #addAllOf(Object...)}.
	 * 
	 * @param allOf
	 * 	The new values to add to the <property>allOf</property> property on this bean.
	 * 	These can either be individual objects or {@link Collection Collections} of objects.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo allOf(Object...allOf) {
		return addAllOf(allOf);
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 * 
	 * @return The value of the <property>properties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Map<String,Object>> getProperties() {
		return properties;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 * 
	 * @param properties The new value for the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setProperties(Map<String,Map<String,Object>> properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 * 
	 * @param name The property name.
	 * @param propertyProperties The properties of the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo addProperty(String name, Map<String,Object> propertyProperties) {
		if (this.properties == null)
			this.properties = new TreeMap<>();
		this.properties.put(name, propertyProperties);
		return this;
	}

	/**
	 * Synonym for {@link #addProperty(String,Map)}.
	 * 
	 * @param name The property name.
	 * @param propertyProperties The properties of the property.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo property(String name, Object...propertyProperties) {
		if (propertyProperties.length % 2 != 0)
			throw new RuntimeException("Invalid number of arguments passed to SchemaInfo.property(String,Object...)");
		Map<String,Object> m = new LinkedHashMap<>();
		for (int i = 0; i < propertyProperties.length; i += 2)
			m.put(String.valueOf(propertyProperties[i]), propertyProperties[i+1]);
		return addProperty(name, m);
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 * 
	 * @return
	 * 	The value of the <property>additionalProperties</property> property on this bean, or <jk>null</jk> if it
	 * 	is not set.
	 */
	public Map<String,Object> getAdditionalProperties() {
		return additionalProperties;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 * 
	 * @param additionalProperties The new value for the <property>additionalProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setAdditionalProperties(Map<String,Object> additionalProperties) {
		this.additionalProperties = additionalProperties;
		return this;
	}

	/**
	 * Synonym for {@link #setAdditionalProperties(Map)}.
	 * 
	 * @param additionalProperties The new value for the <property>additionalProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo additionalProperties(Object...additionalProperties) {
		if (additionalProperties.length % 2 != 0)
			throw new RuntimeException("Invalid number of arguments passed to SchemaInfo.additionalProperties(Object...)");
		Map<String,Object> m = new LinkedHashMap<>();
		for (int i = 0; i < additionalProperties.length; i += 2)
			m.put(String.valueOf(additionalProperties[i]), additionalProperties[i+1]);
		return setAdditionalProperties(m);
	}

	/**
	 * Bean property getter:  <property>discriminator</property>.
	 * 
	 * @return
	 * 	The value of the <property>discriminator</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public String getDiscriminator() {
		return discriminator;
	}

	/**
	 * Bean property setter:  <property>discriminator</property>.
	 * 
	 * @param discriminator The new value for the <property>discriminator</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setDiscriminator(String discriminator) {
		this.discriminator = discriminator;
		return this;
	}

	/**
	 * Synonym for {@link #setDiscriminator(String)}.
	 * 
	 * @param discriminator The new value for the <property>discriminator</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo discriminator(String discriminator) {
		return setDiscriminator(discriminator);
	}

	/**
	 * Bean property getter:  <property>readOnly</property>.
	 * 
	 * @return The value of the <property>readOnly</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getReadOnly() {
		return readOnly;
	}

	/**
	 * Bean property setter:  <property>readOnly</property>.
	 * 
	 * @param readOnly The new value for the <property>readOnly</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	/**
	 * Synonym for {@link #setReadOnly(Boolean)}.
	 * 
	 * @param readOnly The new value for the <property>readOnly</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo readOnly(Boolean readOnly) {
		return setReadOnly(readOnly);
	}

	/**
	 * Bean property getter:  <property>xml</property>.
	 * 
	 * @return The value of the <property>xml</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Xml getXml() {
		return xml;
	}

	/**
	 * Bean property setter:  <property>xml</property>.
	 * 
	 * @param xml The new value for the <property>xml</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setXml(Xml xml) {
		this.xml = xml;
		return this;
	}

	/**
	 * Synonym for {@link #setXml(Xml)}.
	 * 
	 * @param xml The new value for the <property>xml</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo xml(Xml xml) {
		return setXml(xml);
	}

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 * 
	 * @return
	 * 	The value of the <property>externalDocs</property> property on this bean, or <jk>null</jk> if it is not
	 * 	set.
	 */
	public ExternalDocumentation getExternalDocs() {
		return externalDocs;
	}

	/**
	 * Bean property setter:  <property>externalDocs</property>.
	 * 
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExternalDocs(ExternalDocumentation externalDocs) {
		this.externalDocs = externalDocs;
		return this;
	}

	/**
	 * Synonym for {@link #setExternalDocs(ExternalDocumentation)}.
	 * 
	 * @param externalDocs The new value for the <property>externalDocs</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo externalDocs(ExternalDocumentation externalDocs) {
		return setExternalDocs(externalDocs);
	}

	/**
	 * Bean property getter:  <property>example</property>.
	 * 
	 * @return The value of the <property>example</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getExample() {
		return example;
	}

	/**
	 * Bean property setter:  <property>example</property>.
	 * 
	 * @param example The new value for the <property>example</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo setExample(Object example) {
		this.example = example;
		return this;
	}

	/**
	 * Synonym for {@link #setExample(Object)}.
	 * 
	 * @param example The new value for the <property>example</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SchemaInfo example(Object example) {
		return setExample(example);
	}
}
