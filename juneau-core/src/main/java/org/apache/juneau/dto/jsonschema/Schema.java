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
package org.apache.juneau.dto.jsonschema;

import java.net.URI;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Represents a top-level schema object bean in the JSON-Schema core specification.
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.jsonschema} for usage information.
 */
@SuppressWarnings("hiding")
@Bean(typeName="schema",
	properties="id,$schema,$ref, title,description,type,definitions,properties,"
		+ "patternProperties,dependencies,items,multipleOf,maximum,exclusiveMaximum,"
		+ "minimum,exclusiveMinimum,maxLength,minLength,pattern,additionalItems,"
		+ "maxItems,minItems,uniqueItems,maxProperties,minProperties,required,"
		+ "additionalProperties,enum,allOf,anyOf,oneOf,not"
)
public class Schema {
	private String name;                                   // Property name.  Not serialized.
	private URI id;
	private URI schemaVersion;
	private String title;
	private String description;
	private JsonType typeJsonType;                         // JsonType representation of type
	private JsonTypeArray typeJsonTypeArray;               // JsonTypeArray representation of type
	private Map<String,Schema> definitions;
	private Map<String,Schema> properties;
	private Map<String,Schema> patternProperties;
	private Map<String,Schema> dependencies;
	private Schema itemsSchema;                            // Schema representation of items
	private SchemaArray itemsSchemaArray;                  // SchemaArray representation of items
	private Number multipleOf;
	private Number maximum;
	private Boolean exclusiveMaximum;
	private Number minimum;
	private Boolean exclusiveMinimum;
	private Integer maxLength;
	private Integer minLength;
	private String pattern;
	private Boolean additionalItemsBoolean;                // Boolean representation of additionalItems
	private SchemaArray additionalItemsSchemaArray;        // SchemaArray representation of additionalItems
	private Integer maxItems;
	private Integer minItems;
	private Boolean uniqueItems;
	private Integer maxProperties;
	private Integer minProperties;
	private List<String> required;
	private Boolean additionalPropertiesBoolean;           // Boolean representation of additionalProperties
	private Schema additionalPropertiesSchema;             // Schema representation of additionalProperties
	private List<String> _enum;
	private List<Schema> allOf;
	private List<Schema> anyOf;
	private List<Schema> oneOf;
	private Schema not;
	private URI ref;
	private SchemaMap schemaMap;
	private Schema master = this;

	/**
	 * Default constructor.
	 */
	public Schema() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanIgnore
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanIgnore
	public Schema setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * @return The value of the <property>id</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public URI getId() {
		return id;
	}

	/**
	 * Bean property setter:  <property>id</property>.
	 *
	 * @param id The new value for the <property>id</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setId(URI id) {
		this.id = id;
		return this;
	}

	/**
	 * Bean property setter:  <property>id</property>.
	 *
	 * @param id The new value for the <property>id</property> property on this bean.
	 * 	The parameter must be a valid URI.  It can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Schema setId(String id) {
		return setId(id == null ? null : URI.create(id));
	}

	/**
	 * Bean property getter:  <property>$schema</property>.
	 *
	 * @return The value of the <property>$schema</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty(name="$schema")
	public URI getSchemaVersionUri() {
		return schemaVersion;
	}

	/**
	 * Bean property setter:  <property>$schema</property>.
	 *
	 * @param schemaVersion The new value for the <property>schemaVersion</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="$schema")
	public Schema setSchemaVersionUri(URI schemaVersion) {
		this.schemaVersion = schemaVersion;
		return this;
	}

	/**
	 * Bean property setter:  <property>schemaVersion</property>.
	 *
	 * @param schemaVersion The new value for the <property>schemaVersion</property> property on this bean.
	 * 	The parameter must be a valid URI.  It can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Schema setSchemaVersionUri(String schemaVersion) {
		return setSchemaVersionUri(schemaVersion == null ? null : URI.create(schemaVersion));
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * @return The value of the <property>title</property> property, or <jk>null</jk> if it is not set.
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
	public Schema setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The value of the <property>description</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link JsonType} or {@link JsonTypeArray} depending on what value was used to set it.
	 */
	@BeanProperty(swap=JsonTypeOrJsonTypeArraySwap.class)
	public Object getType() {
		if (typeJsonType != null)
			return typeJsonType;
		return typeJsonTypeArray;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * <p>
	 * Convenience method for returning the <property>type</property> property when it is a {@link JsonType} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonTypeArray}.
	 */
	@BeanIgnore
	public JsonType getTypeAsJsonType() {
		return typeJsonType;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * <p>
	 * Convenience method for returning the <property>type</property> property when it is a {@link JsonTypeArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonType}.
	 */
	@BeanIgnore
	public JsonTypeArray getTypeAsJsonTypeArray() {
		return typeJsonTypeArray;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * 	This object must be of type {@link JsonType} or {@link JsonTypeArray}.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public Schema setType(Object type) {
		this.typeJsonType = null;
		this.typeJsonTypeArray = null;
		if (type != null) {
			if (type instanceof JsonType)
				this.typeJsonType = (JsonType)type;
			else if (type instanceof JsonTypeArray)
				this.typeJsonTypeArray = (JsonTypeArray)type;
			else
				throw new BeanRuntimeException(SchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  SimpleType, SimpleTypeArray", type.getClass().getName());
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>type</property>.
	 *
	 * @param types The list of items to append to the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addTypes(JsonType...types) {
		if (this.typeJsonTypeArray == null)
			this.typeJsonTypeArray = new JsonTypeArray();
		this.typeJsonTypeArray.addAll(types);
		return this;
	}

	/**
	 * Used during parsing to convert the <property>type</property> property to the correct class type.
	 * <ul class='spaced-list'>
	 * 	<li>If parsing a JSON-array, converts to a {@link JsonTypeArray}.
	 * 	<li>If parsing a JSON-object, converts to a {@link JsonType}.
	 * </ul>
	 * Serialization method is a no-op.
	 */
	public static class JsonTypeOrJsonTypeArraySwap extends PojoSwap<Object,Object> {

		@Override /* PojoSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* PojoSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			ClassMeta<?> cm = (o instanceof Collection ? session.getClassMeta(JsonTypeArray.class) : session.getClassMeta(JsonType.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * @return The value of the <property>definitions</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Schema> getDefinitions() {
		return definitions;
	}

	/**
	 * Bean property setter:  <property>definitions</property>.
	 *
	 * @param definitions The new value for the <property>definitions</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setDefinitions(Map<String,Schema> definitions) {
		this.definitions = definitions;
		if (definitions != null)
			setMasterOn(definitions.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * @param name The key in the definitions map entry.
	 * @param definition The value in the definitions map entry.
	 * @return This object (for method chaining).
	 */
	public Schema addDefinition(String name, Schema definition) {
		if (this.definitions == null)
			this.definitions = new LinkedHashMap<String,Schema>();
		this.definitions.put(name, definition);
		setMasterOn(definition);
		return this;
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The value of the <property>properties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Schema> getProperties() {
		return properties;
	}

	/**
	 * Returns the property with the specified name.
	 * This is equivalent to calling <property>getProperty(name, <jk>false</jk>)</property>.
	 *
	 * @param name The property name.
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public Schema getProperty(String name) {
		return getProperty(name, false);
	}

	/**
	 * Returns the property with the specified name.
	 * If <property>resolve</property> is <jk>true</jk>, the property object will automatically be
	 * 	resolved by calling {@link #resolve()}.
	 * Therefore, <property>getProperty(name, <jk>true</jk>)</property> is equivalent to calling
	 * 	<property>getProperty(name).resolve()</property>, except it's safe from a potential <property>NullPointerException</property>.
	 *
	 * @param name The property name.
	 * @param resolve If <jk>true</jk>, calls {@link #resolve()} on object before returning.
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public Schema getProperty(String name, boolean resolve) {
		if (properties == null)
			return null;
		Schema s = properties.get(name);
		if (s == null)
			return null;
		if (resolve)
			s = s.resolve();
		return s;
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param properties The new value for the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setProperties(Map<String,Schema> properties) {
		this.properties = properties;
		if (properties != null)
			for (Map.Entry<String,Schema> e : properties.entrySet()) {
				Schema value = e.getValue();
				setMasterOn(value);
				value.setName(e.getKey());
			}
		return this;
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 * <p>
	 * Properties must have their <property>name</property> property set on them when using this method.
	 *
	 * @param properties The list of items to append to the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public Schema addProperties(Schema...properties) {
		if (this.properties == null)
			this.properties = new LinkedHashMap<String,Schema>();
		for (Schema p : properties) {
			if (p.getName() == null)
				throw new BeanRuntimeException(Schema.class, "Invalid property passed to Schema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.properties.put(p.getName(), p);
		}
		return this;
	}

	/**
	 * Bean property getter:  <property>patternProperties</property>.
	 *
	 * @return The value of the <property>patternProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Schema> getPatternProperties() {
		return patternProperties;
	}

	/**
	 * Bean property setter:  <property>patternProperties</property>.
	 *
	 * @param patternProperties The new value for the <property>patternProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setPatternProperties(Map<String,Schema> patternProperties) {
		this.patternProperties = patternProperties;
		if (patternProperties != null)
			for (Map.Entry<String,Schema> e : patternProperties.entrySet()) {
				Schema s = e.getValue();
				setMasterOn(s);
				s.setName(e.getKey());
			}
		return this;
	}

	/**
	 * Bean property appender:  <property>patternProperties</property>.
	 * <p>
	 * Properties must have their <property>name</property> property set to the pattern string when using this method.
	 *
	 * @param properties The list of items to append to the <property>patternProperties</property> property on this bean.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public Schema addPatternProperties(SchemaProperty...properties) {
		if (this.patternProperties == null)
			this.patternProperties = new LinkedHashMap<String,Schema>();
		for (Schema p : properties) {
			if (p.getName() == null)
				throw new BeanRuntimeException(Schema.class, "Invalid property passed to Schema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.patternProperties.put(p.getName(), p);
		}
		return this;
	}

	/**
	 * Bean property getter:  <property>dependencies</property>.
	 *
	 * @return The value of the <property>dependencies</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Schema> getDependencies() {
		return dependencies;
	}

	/**
	 * Bean property setter:  <property>dependencies</property>.
	 *
	 * @param dependencies The new value for the <property>dependencies</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setDependencies(Map<String,Schema> dependencies) {
		this.dependencies = dependencies;
		if (dependencies != null)
			setMasterOn(dependencies.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>dependencies</property>.
	 *
	 * @param name The key of the entry in the dependencies map.
	 * @param dependency The value of the entry in the dependencies map.
	 * @return This object (for method chaining).
	 */
	public Schema addDependency(String name, Schema dependency) {
		if (this.dependencies == null)
			this.dependencies = new LinkedHashMap<String,Schema>();
		this.dependencies.put(name, dependency);
		setMasterOn(dependency);
		return this;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * @return The value of the <property>items</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link Schema} or {@link SchemaArray} depending on what value was used to set it.
	 */
	@BeanProperty(swap=SchemaOrSchemaArraySwap.class)
	public Object getItems() {
		if (itemsSchema != null)
			return itemsSchema;
		return itemsSchemaArray;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * <p>
	 * Convenience method for returning the <property>items</property> property when it is a {@link Schema} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link SchemaArray}.
	 */
	@BeanIgnore
	public Schema getItemsAsSchema() {
		return itemsSchema;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * <p>
	 * Convenience method for returning the <property>items</property> property when it is a {@link SchemaArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Schema}.
	 */
	@BeanIgnore
	public SchemaArray getItemsAsSchemaArray() {
		return itemsSchemaArray;
	}

	/**
	 * Used during parsing to convert the <property>items</property> property to the correct class type.
	 * <ul class='spaced-list'>
	 * 	<li>If parsing a JSON-array, converts to a {@link SchemaArray}.
	 * 	<li>If parsing a JSON-object, converts to a {@link Schema}.
	 * </ul>
	 * Serialization method is a no-op.
	 */
	public static class SchemaOrSchemaArraySwap extends PojoSwap<Object,Object> {

		@Override /* PojoSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* PojoSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			ClassMeta<?> cm = (o instanceof Collection ? session.getClassMeta(SchemaArray.class) : session.getClassMeta(Schema.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * @param items The new value for the <property>items</property> property on this bean.
	 * 	This object must be of type {@link Schema} or {@link SchemaArray}.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public Schema setItems(Object items) {
		this.itemsSchema = null;
		this.itemsSchemaArray = null;
		if (items != null) {
			if (items instanceof Schema) {
				this.itemsSchema = (Schema)items;
				setMasterOn(this.itemsSchema);
			} else if (items instanceof SchemaArray) {
				this.itemsSchemaArray = (SchemaArray)items;
				setMasterOn(this.itemsSchemaArray);
			} else
				throw new BeanRuntimeException(SchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  Schema, SchemaArray", items.getClass().getName());
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>items</property>.
	 *
	 * @param items The list of items to append to the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addItems(Schema...items) {
		if (this.itemsSchemaArray == null)
			this.itemsSchemaArray = new SchemaArray();
		this.itemsSchemaArray.addAll(items);
		setMasterOn(items);
		return this;
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
	public Schema setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
		return this;
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
	public Schema setMaximum(Number maximum) {
		this.maximum = maximum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The value of the <property>exclusiveMaximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
		return this;
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
	public Schema setMinimum(Number minimum) {
		this.minimum = minimum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The value of the <property>exclusiveMinimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
		return this;
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
	public Schema setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
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
	public Schema setMinLength(Integer minLength) {
		this.minLength = minLength;
		return this;
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
	public Schema setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 *
	 * @return The value of the <property>additionalItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link Boolean} or {@link SchemaArray} depending on what value was used to set it.
	 */
	@BeanProperty(swap=BooleanOrSchemaArraySwap.class)
	public Object getAdditionalItems() {
		if (additionalItemsBoolean != null)
			return additionalItemsBoolean;
		return additionalItemsSchemaArray;
	}

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 * <p>
	 * Convenience method for returning the <property>additionalItems</property> property when it is a {@link Boolean} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link SchemaArray}.
	 */
	@BeanIgnore
	public Boolean getAdditionalItemsAsBoolean() {
		return additionalItemsBoolean;
	}

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 * <p>
	 * Convenience method for returning the <property>additionalItems</property> property when it is a {@link SchemaArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Boolean}.
	 */
	@BeanIgnore
	public List<Schema> getAdditionalItemsAsSchemaArray() {
		return additionalItemsSchemaArray;
	}

	/**
	 * Bean property setter:  <property>additionalItems</property>.
	 *
	 * @param additionalItems The new value for the <property>additionalItems</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link SchemaArray}.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public Schema setAdditionalItems(Object additionalItems) {
		this.additionalItemsBoolean = null;
		this.additionalItemsSchemaArray = null;
		if (additionalItems != null) {
			if (additionalItems instanceof Boolean)
				this.additionalItemsBoolean = (Boolean)additionalItems;
			else if (additionalItems instanceof SchemaArray) {
				this.additionalItemsSchemaArray = (SchemaArray)additionalItems;
				setMasterOn(this.additionalItemsSchemaArray);
			} else
				throw new BeanRuntimeException(SchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  Boolean, SchemaArray", additionalItems.getClass().getName());
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>additionalItems</property>.
	 *
	 * @param additionalItems The list of items to append to the <property>additionalItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addAdditionalItems(Schema...additionalItems) {
		if (this.additionalItemsSchemaArray == null)
			this.additionalItemsSchemaArray = new SchemaArray();
		this.additionalItemsSchemaArray.addAll(additionalItems);
		setMasterOn(additionalItems);
		return this;
	}

	/**
	 * Used during parsing to convert the <property>additionalItems</property> property to the correct class type.
	 * <ul class='spaced-list'>
	 * 	<li>If parsing a JSON-array, converts to a {@link SchemaArray}.
	 * 	<li>If parsing a JSON-boolean, converts to a {@link Boolean}.
	 * </ul>
	 * Serialization method is a no-op.
	 */
	public static class BooleanOrSchemaArraySwap extends PojoSwap<Object,Object> {

		@Override /* PojoSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* PojoSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			ClassMeta<?> cm = (o instanceof Collection ? session.getClassMeta(SchemaArray.class) : session.getClassMeta(Boolean.class));
			return session.convertToType(o, cm);
		}
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
	public Schema setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
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
	public Schema setMinItems(Integer minItems) {
		this.minItems = minItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not set.
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
	public Schema setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 *
	 * @return The value of the <property>maxProperties</property> property on this bean, or <jk>null</jk> if it is not set.
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
	public Schema setMaxProperties(Integer maxProperties) {
		this.maxProperties = maxProperties;
		return this;
	}

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 *
	 * @return The value of the <property>minProperties</property> property on this bean, or <jk>null</jk> if it is not set.
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
	public Schema setMinProperties(Integer minProperties) {
		this.minProperties = minProperties;
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * @return The value of the <property>required</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 *
	 * @param required The new value for the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setRequired(List<String> required) {
		this.required = required;
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param required The list of items to append to the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addRequired(List<String> required) {
		if (this.required == null)
			this.required = new LinkedList<String>();
		for (String r : required)
			this.required.add(r);
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param required The list of items to append to the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addRequired(String...required) {
		if (this.required == null)
			this.required = new LinkedList<String>();
		for (String r : required)
			this.required.add(r);
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param properties The list of items to append to the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addRequired(SchemaProperty...properties) {
		if (this.required == null)
			this.required = new LinkedList<String>();
		for (SchemaProperty p : properties)
			this.required.add(p.getName());
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return The value of the <property>additionalProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link Boolean} or {@link SchemaArray} depending on what value was used to set it.
	 */
	@BeanProperty(swap=BooleanOrSchemaSwap.class)
	public Object getAdditionalProperties() {
		if (additionalPropertiesBoolean != null)
			return additionalItemsBoolean;
		return additionalPropertiesSchema;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 * <p>
	 * Convenience method for returning the <property>additionalProperties</property> property when it is a {@link Boolean} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Schema}.
	 */
	@BeanIgnore
	public Boolean getAdditionalPropertiesAsBoolean() {
		return additionalPropertiesBoolean;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 * <p>
	 * Convenience method for returning the <property>additionalProperties</property> property when it is a {@link Schema} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Boolean}.
	 */
	@BeanIgnore
	public Schema getAdditionalPropertiesAsSchema() {
		return additionalPropertiesSchema;
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param additionalProperties The new value for the <property>additionalProperties</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link Schema}.
	 * @return This object (for method chaining).
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	@BeanProperty(beanDictionary={Schema.class})
	public Schema setAdditionalProperties(Object additionalProperties) {
		this.additionalPropertiesBoolean = null;
		this.additionalPropertiesSchema = null;
		if (additionalProperties != null) {
			if (additionalProperties instanceof Boolean)
				this.additionalPropertiesBoolean = (Boolean)additionalProperties;
			else if (additionalProperties instanceof Schema) {
				this.additionalPropertiesSchema = (Schema)additionalProperties;
				setMasterOn(this.additionalPropertiesSchema);
			} else
				throw new BeanRuntimeException(SchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  Boolean, Schema", additionalProperties.getClass().getName());
		}
		return this;
	}

	/**
	 * Used during parsing to convert the <property>additionalProperties</property> property to the correct class type.
	 * <ul class='spaced-list'>
	 * 	<li>If parsing a JSON-object, converts to a {@link Schema}.
	 * 	<li>If parsing a JSON-boolean, converts to a {@link Boolean}.
	 * </ul>
	 * Serialization method is a no-op.
	 */
	public static class BooleanOrSchemaSwap extends PojoSwap<Object,Object> {

		@Override /* PojoSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* PojoSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			ClassMeta<?> cm = (o instanceof Boolean ? session.getClassMeta(Boolean.class) : session.getClassMeta(Schema.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setEnum(List<String> _enum) {
		this._enum = _enum;
		return this;
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param _enum The list of items to append to the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addEnum(String..._enum) {
		if (this._enum == null)
			this._enum = new LinkedList<String>();
		for (String e : _enum)
			this._enum.add(e);
		return this;
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The value of the <property>allOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Schema> getAllOf() {
		return allOf;
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param allOf The new value for the <property>allOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setAllOf(List<Schema> allOf) {
		this.allOf = allOf;
		setMasterOn(allOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param allOf The list of items to append to the <property>allOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addAllOf(Schema...allOf) {
		if (this.allOf == null)
			this.allOf = new LinkedList<Schema>();
		setMasterOn(allOf);
		for (Schema s : allOf)
			this.allOf.add(s);
		return this;
	}

	/**
	 * Bean property getter:  <property>anyOf</property>.
	 *
	 * @return The value of the <property>anyOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Schema> getAnyOf() {
		return anyOf;
	}

	/**
	 * Bean property setter:  <property>anyOf</property>.
	 *
	 * @param anyOf The new value of the <property>anyOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setAnyOf(List<Schema> anyOf) {
		this.anyOf = anyOf;
		setMasterOn(anyOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>anyOf</property>.
	 *
	 * @param anyOf The list of items to append to the <property>anyOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addAnyOf(Schema...anyOf) {
		if (this.anyOf == null)
			this.anyOf = new LinkedList<Schema>();
		setMasterOn(anyOf);
		for (Schema s : anyOf)
			this.anyOf.add(s);
		return this;
	}

	/**
	 * Bean property getter:  <property>oneOf</property>.
	 *
	 * @return The value of the <property>oneOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Schema> getOneOf() {
		return oneOf;
	}

	/**
	 * Bean property setter:  <property>oneOf</property>.
	 *
	 * @param oneOf The new value for the <property>oneOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setOneOf(List<Schema> oneOf) {
		this.oneOf = oneOf;
		setMasterOn(oneOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>oneOf</property>.
	 *
	 * @param oneOf The list of items to append to the <property>oneOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema addOneOf(Schema...oneOf) {
		if (this.oneOf == null)
			this.oneOf = new LinkedList<Schema>();
		setMasterOn(oneOf);
		for (Schema s : oneOf)
			this.oneOf.add(s);
		return this;
	}

	/**
	 * Bean property getter:  <property>not</property>.
	 *
	 * @return The value of the <property>not</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Schema getNot() {
		return not;
	}

	/**
	 * Bean property setter:  <property>not</property>.
	 *
	 * @param not The new value for the <property>not</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Schema setNot(Schema not) {
		this.not = not;
		setMasterOn(not);
		return this;
	}

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The value of the <property>$ref</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty(name="$ref")
	public URI getRef() {
		return ref;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param ref The new value for the <property>$ref</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="$ref")
	public Schema setRef(URI ref) {
		this.ref = ref;
		return this;
	}

	private void setMasterOn(Schema s) {
		if (s != null)
			s.setMaster(this);
	}

	private void setMasterOn(Schema[] ss) {
		if (ss != null)
			for (Schema s : ss)
				setMasterOn(s);
	}

	private void setMasterOn(Collection<Schema> ss) {
		if (ss != null)
			for (Schema s : ss)
				setMasterOn(s);
	}

	private void setMasterOn(SchemaArray ss) {
		if (ss != null)
			for (Schema s : ss)
				setMasterOn(s);
	}

	/**
	 * Sets the master schema for this schema and all child schema objects.
	 * <p>
	 * All child elements in a schema should point to a single "master" schema in order to
	 * 	locate registered SchemaMap objects for resolving external schemas.
	 *
	 * @param master The master schema to associate on this and all children.  Can be <jk>null</jk>.
	 */
	protected void setMaster(Schema master) {
		this.master = master;
		if (definitions != null)
			for (Schema s : definitions.values())
				s.setMaster(master);
		if (properties != null)
			for (Schema s : properties.values())
				s.setMaster(master);
		if (patternProperties != null)
			for (Schema s : patternProperties.values())
				s.setMaster(master);
		if (dependencies != null)
			for (Schema s : dependencies.values())
				s.setMaster(master);
		if (itemsSchema != null)
			itemsSchema.setMaster(master);
		if (itemsSchemaArray != null)
			for (Schema s : itemsSchemaArray)
				s.setMaster(master);
		if (additionalItemsSchemaArray != null)
			for (Schema s : additionalItemsSchemaArray)
				s.setMaster(master);
		if (additionalPropertiesSchema != null)
			additionalPropertiesSchema.setMaster(master);
		if (allOf != null)
			for (Schema s : allOf)
				s.setMaster(master);
		if (anyOf != null)
			for (Schema s : anyOf)
				s.setMaster(master);
		if (oneOf != null)
			for (Schema s : oneOf)
				s.setMaster(master);
		if (not != null)
			not.setMaster(master);
	}


	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param ref The new value for the <property>$ref</property> property on this bean.
	 * 	The parameter must be a valid URI.  It can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Schema setRef(String ref) {
		return setRef(ref == null ? null : URI.create(ref));
	}

	/**
	 * If this schema is a reference to another schema (i.e. has its <property>$ref</property> property set),
	 * 	this method will retrieve the referenced schema from the schema map registered with this schema.
	 * If this schema is not a reference, or no schema map is registered with this schema, this method
	 * 	is a no-op and simply returns this object.
	 *
	 * @return The referenced schema, or <jk>null</jk>.
	 */
	public Schema resolve() {
		if (ref == null || master.schemaMap == null)
			return this;
		return master.schemaMap.get(ref);
	}

	/**
	 * Associates a schema map with this schema for resolving other schemas identified
	 * 	through <property>$ref</property> properties.
	 *
	 * @param schemaMap The schema map to associate with this schema.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public Schema setSchemaMap(SchemaMap schemaMap) {
		this.schemaMap = schemaMap;
		return this;
	}
}
