/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.jsonschema;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;

/**
 * Represents a top-level schema object bean in the JSON-Schema core specification.
 *
 * <p>
 * This implementation follows the JSON Schema Draft 2020-12 specification.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a simple schema for a person object</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setIdUri(<js>"https://example.com/person.schema.json"</js>)
 * 		.setSchemaVersionUri(<js>"https://json-schema.org/draft/2020-12/schema"</js>)
 * 		.setTitle(<js>"Person"</js>)
 * 		.setDescription(<js>"A person object"</js>)
 * 		.setType(JsonType.<jsf>OBJECT</jsf>)
 * 		.addProperties(
 * 			<jk>new</jk> JsonSchemaProperty(<js>"firstName"</js>, JsonType.<jsf>STRING</jsf>)
 * 				.setMinLength(1)
 * 				.setMaxLength(50),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"lastName"</js>, JsonType.<jsf>STRING</jsf>)
 * 				.setMinLength(1)
 * 				.setMaxLength(50),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"age"</js>, JsonType.<jsf>INTEGER</jsf>)
 * 				.setMinimum(0)
 * 				.setExclusiveMaximum(150)
 * 		)
 * 		.addRequired(<js>"firstName"</js>, <js>"lastName"</js>);
 *
 * 	<jc>// Serialize to JSON Schema</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT_SORTED</jsf>.serialize(<jv>schema</jv>);
 * </p>
 *
 * <p>
 * Output:
 * <p class='bjson'>
 * 	{
 * 		<js>"$id"</js>: <js>"https://example.com/person.schema.json"</js>,
 * 		<js>"$schema"</js>: <js>"https://json-schema.org/draft/2020-12/schema"</js>,
 * 		<js>"title"</js>: <js>"Person"</js>,
 * 		<js>"description"</js>: <js>"A person object"</js>,
 * 		<js>"type"</js>: <js>"object"</js>,
 * 		<js>"properties"</js>: {
 * 			<js>"firstName"</js>: {
 * 				<js>"type"</js>: <js>"string"</js>,
 * 				<js>"minLength"</js>: 1,
 * 				<js>"maxLength"</js>: 50
 * 			},
 * 			<js>"lastName"</js>: {
 * 				<js>"type"</js>: <js>"string"</js>,
 * 				<js>"minLength"</js>: 1,
 * 				<js>"maxLength"</js>: 50
 * 			},
 * 			<js>"age"</js>: {
 * 				<js>"type"</js>: <js>"integer"</js>,
 * 				<js>"minimum"</js>: 0,
 * 				<js>"exclusiveMaximum"</js>: 150
 * 			}
 * 		},
 * 		<js>"required"</js>: [<js>"firstName"</js>, <js>"lastName"</js>]
 * 	}
 * </p>
 *
 * <h5 class='section'>Key Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Draft 2020-12 Support:</b> Includes all properties from the latest JSON Schema specification
 * 	<li><b>Backward Compatibility:</b> Deprecated Draft 04 properties (like <c>id</c> and <c>definitions</c>) are still supported
 * 	<li><b>Fluent API:</b> All setter methods return <c>this</c> for method chaining
 * 	<li><b>Type Safety:</b> Uses enums and typed collections for validation
 * 	<li><b>Serialization:</b> Can be serialized to any format supported by Juneau (JSON, XML, HTML, etc.)
 * </ul>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 *
 * <p><b>1. Simple Type Constraints:</b>
 * <p class='bjava'>
 * 	<jc>// String with length constraints</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>STRING</jsf>)
 * 		.setMinLength(5)
 * 		.setMaxLength(100)
 * 		.setPattern(<js>"^[A-Za-z]+$"</js>);
 * </p>
 *
 * <p><b>2. Numeric Ranges:</b>
 * <p class='bjava'>
 * 	<jc>// Number between 0 and 100 (exclusive)</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>NUMBER</jsf>)
 * 		.setExclusiveMinimum(0)
 * 		.setExclusiveMaximum(100)
 * 		.setMultipleOf(0.5);
 * </p>
 *
 * <p><b>3. Enumerations:</b>
 * <p class='bjava'>
 * 	<jc>// Status field with allowed values</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>STRING</jsf>)
 * 		.addEnum(<js>"pending"</js>, <js>"active"</js>, <js>"completed"</js>);
 * </p>
 *
 * <p><b>4. Arrays:</b>
 * <p class='bjava'>
 * 	<jc>// Array of strings with constraints</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>ARRAY</jsf>)
 * 		.setItems(<jk>new</jk> JsonSchema().setType(JsonType.<jsf>STRING</jsf>))
 * 		.setMinItems(1)
 * 		.setMaxItems(10)
 * 		.setUniqueItems(<jk>true</jk>);
 * </p>
 *
 * <p><b>5. Conditional Schemas (Draft 07+):</b>
 * <p class='bjava'>
 * 	<jc>// Different validation based on country</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>OBJECT</jsf>)
 * 		.addProperties(
 * 			<jk>new</jk> JsonSchemaProperty(<js>"country"</js>, JsonType.<jsf>STRING</jsf>),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"postalCode"</js>, JsonType.<jsf>STRING</jsf>)
 * 		)
 * 		.setIf(<jk>new</jk> JsonSchema()
 * 			.addProperties(<jk>new</jk> JsonSchemaProperty(<js>"country"</js>).setConst(<js>"USA"</js>))
 * 		)
 * 		.setThen(<jk>new</jk> JsonSchema()
 * 			.addProperties(<jk>new</jk> JsonSchemaProperty(<js>"postalCode"</js>).setPattern(<js>"^[0-9]{5}$"</js>))
 * 		);
 * </p>
 *
 * <p><b>6. Reusable Definitions:</b>
 * <p class='bjava'>
 * 	<jc>// Schema with reusable components using $defs</jc>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema()
 * 		.setType(JsonType.<jsf>OBJECT</jsf>)
 * 		.addDef(<js>"address"</js>, <jk>new</jk> JsonSchema()
 * 			.setType(JsonType.<jsf>OBJECT</jsf>)
 * 			.addProperties(
 * 				<jk>new</jk> JsonSchemaProperty(<js>"street"</js>, JsonType.<jsf>STRING</jsf>),
 * 				<jk>new</jk> JsonSchemaProperty(<js>"city"</js>, JsonType.<jsf>STRING</jsf>)
 * 			)
 * 		)
 * 		.addProperties(
 * 			<jk>new</jk> JsonSchemaProperty(<js>"billingAddress"</js>)
 * 				.setRef(<js>"#/$defs/address"</js>),
 * 			<jk>new</jk> JsonSchemaProperty(<js>"shippingAddress"</js>)
 * 				.setRef(<js>"#/$defs/address"</js>)
 * 		);
 * </p>
 *
 * <h5 class='section'>Migration from Draft 04:</h5>
 * <ul class='spaced-list'>
 * 	<li>Use {@link #setIdUri(Object)} instead of {@link #setId(Object)} (deprecated)
 * 	<li>Use {@link #setDefs(Map)} instead of {@link #setDefinitions(Map)} (deprecated but still works)
 * 	<li>Use {@link #setExclusiveMaximum(Number)} with a numeric value instead of a boolean flag
 * 	<li>Use {@link #setExclusiveMinimum(Number)} with a numeric value instead of a boolean flag
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a href="https://json-schema.org/draft/2020-12/json-schema-core.html">JSON Schema 2020-12 Core</a>
 * 	<li class='link'><a href="https://json-schema.org/draft/2020-12/json-schema-validation.html">JSON Schema 2020-12 Validation</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonSchema">juneau-bean-jsonschema</a>
 * </ul>
 */
@Bean(typeName = "schema")
public class JsonSchema {
	// @formatter:off
	private String name;                                   // Property name.  Not serialized.
	private URI idUri;                                     // Draft 2020-12: $id
	private URI id;                                        // Draft 04: id (deprecated but kept for compatibility)
	private URI schemaVersion;
	private String title;
	private String description;
	private JsonType typeJsonType;                         // JsonType representation of type
	private JsonTypeArray typeJsonTypeArray;               // JsonTypeArray representation of type
	private Map<String,JsonSchema> definitions;            // Retained for backward compatibility
	private Map<String,JsonSchema> defs;                   // Draft 2020-12: $defs
	private Map<String,JsonSchema> properties;
	private Map<String,JsonSchema> patternProperties;
	private Map<String,JsonSchema> dependencies;           // Retained for backward compatibility
	private Map<String,JsonSchema> dependentSchemas;       // Draft 2019-09+
	private Map<String,List<String>> dependentRequired;    // Draft 2019-09+
	private JsonSchema itemsSchema;                        // JsonSchema representation of items
	private JsonSchemaArray itemsSchemaArray;              // JsonSchemaArray representation of items
	private JsonSchemaArray prefixItems;                   // Draft 2020-12: replaces tuple validation
	private Number multipleOf;
	private Number maximum;
	private Number exclusiveMaximum;                       // Draft 06+: changed from Boolean to Number
	private Number minimum;
	private Number exclusiveMinimum;                       // Draft 06+: changed from Boolean to Number
	private Integer maxLength;
	private Integer minLength;
	private String pattern;
	private Boolean additionalItemsBoolean;                // Boolean representation of additionalItems
	private JsonSchemaArray additionalItemsSchemaArray;    // JsonSchemaArray representation of additionalItems
	private JsonSchema unevaluatedItems;                   // Draft 2019-09+
	private Integer maxItems;
	private Integer minItems;
	private Boolean uniqueItems;
	private Integer maxProperties;
	private Integer minProperties;
	private List<String> required;
	private Boolean additionalPropertiesBoolean;           // Boolean representation of additionalProperties
	private JsonSchema additionalPropertiesSchema;         // JsonSchema representation of additionalProperties
	private JsonSchema unevaluatedProperties;              // Draft 2019-09+
	private List<Object> _enum;                            // NOSONAR - Intentional naming. Changed to Object to support any type
	private Object _const;                                 // NOSONAR - Intentional naming. Draft 06+
	private List<Object> examples;                         // Draft 06+
	private List<JsonSchema> allOf;
	private List<JsonSchema> anyOf;
	private List<JsonSchema> oneOf;
	private JsonSchema not;
	private JsonSchema _if;                                // NOSONAR - Intentional naming. Draft 07+
	private JsonSchema _then;                              // NOSONAR - Intentional naming. Draft 07+
	private JsonSchema _else;                              // NOSONAR - Intentional naming. Draft 07+
	private Boolean readOnly;                              // Draft 07+
	private Boolean writeOnly;                             // Draft 07+
	private String contentMediaType;                       // Draft 07+
	private String contentEncoding;                        // Draft 07+
	private URI ref;
	private JsonSchemaMap schemaMap;
	private JsonSchema master = this;
	// @formatter:on

	/**
	 * Default constructor.
	 */
	public JsonSchema() { /* Empty constructor. */ }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * This is an internal property used for tracking property names and is not part of the JSON Schema specification.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanIgnore
	public String getName() { return name; }

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * This is an internal property used for tracking property names and is not part of the JSON Schema specification.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object.
	 */
	@BeanIgnore
	public JsonSchema setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>$id</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 property for schema identification.
	 *
	 * @return The value of the <property>$id</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$id")
	public URI getIdUri() {
		return idUri; // Return only idUri, not id (to avoid double serialization)
	}

	/**
	 * Bean property setter:  <property>$id</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 property for schema identification.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param idUri The new value for the <property>$id</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("$id")
	public JsonSchema setIdUri(Object idUri) {
		this.idUri = toURI(idUri);
		return this;
	}

	/**
	 * Bean property getter:  <property>id</property>.
	 *
	 * <p>
	 * <b>Deprecated:</b> Use {@link #getIdUri()} instead.
	 * This property is retained for Draft 04 backward compatibility.
	 *
	 * @return The value of the <property>id</property> property on this bean, or <jk>null</jk> if it is not set.
	 * @deprecated Use {@link #getIdUri()} instead.
	 */
	@Deprecated
	public URI getId() {
		return nn(id) ? id : idUri; // Fall back to new '$id' for compatibility when reading
	}

	/**
	 * Bean property setter:  <property>id</property>.
	 *
	 * <p>
	 * <b>Deprecated:</b> Use {@link #setIdUri(Object)} instead.
	 * This property is retained for Draft 04 backward compatibility.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param id The new value for the <property>id</property> property on this bean.
	 * @return This object.
	 * @deprecated Use {@link #setIdUri(Object)} instead.
	 */
	@Deprecated
	public JsonSchema setId(Object id) {
		this.id = toURI(id);
		return this;
	}

	/**
	 * Bean property getter:  <property>$schema</property>.
	 *
	 * @return The value of the <property>$schema</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$schema")
	public URI getSchemaVersionUri() { return schemaVersion; }

	/**
	 * Bean property setter:  <property>$schema</property>.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param schemaVersion The new value for the <property>schemaVersion</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("$schema")
	public JsonSchema setSchemaVersionUri(Object schemaVersion) {
		this.schemaVersion = toURI(schemaVersion);
		return this;
	}

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * @return The value of the <property>title</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() { return title; }

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * @param title The new value for the <property>title</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The value of the <property>description</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * @return
	 * 	The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link JsonType} or {@link JsonTypeArray} depending on what value was used to set it.
	 */
	@Swap(JsonTypeOrJsonTypeArraySwap.class)
	public Object getType() {
		if (nn(typeJsonType))
			return typeJsonType;
		return typeJsonTypeArray;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>type</property> property when it is a {@link JsonType} value.
	 *
	 * @return
	 * 	The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonTypeArray}.
	 */
	@BeanIgnore
	public JsonType getTypeAsJsonType() { return typeJsonType; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>type</property> property when it is a {@link JsonTypeArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonType}.
	 */
	@BeanIgnore
	public JsonTypeArray getTypeAsJsonTypeArray() { return typeJsonTypeArray; }

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param type
	 * 	The new value for the <property>type</property> property on this bean.
	 * 	This object must be of type {@link JsonType} or {@link JsonTypeArray}.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public JsonSchema setType(Object type) {
		this.typeJsonType = null;
		this.typeJsonTypeArray = null;
		if (nn(type)) {
			if (type instanceof JsonType type2)
				this.typeJsonType = type2;
			else if (type instanceof JsonTypeArray type2)
				this.typeJsonTypeArray = type2;
			else
				throw bex(JsonSchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  SimpleType, SimpleTypeArray", cn(type));
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>type</property>.
	 *
	 * @param types The list of items to append to the <property>type</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addTypes(JsonType...types) {
		if (this.typeJsonTypeArray == null)
			this.typeJsonTypeArray = new JsonTypeArray();
		this.typeJsonTypeArray.addAll(types);
		return this;
	}

	/**
	 * Used during parsing to convert the <property>type</property> property to the correct class type.
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If parsing a JSON-array, converts to a {@link JsonTypeArray}.
	 * 	<li>
	 * 		If parsing a JSON-object, converts to a {@link JsonType}.
	 * </ul>
	 *
	 * <p>
	 * Serialization method is a no-op.
	 */
	public static class JsonTypeOrJsonTypeArraySwap extends ObjectSwap<Object,Object> {

		@Override /* Overridden from ObjectSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonTypeArray.class) : session.getClassMeta(JsonType.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property getter:  <property>definitions</property>.
	 *
	 * <p>
	 * <b>Deprecated:</b> Use {@link #getDefs()} for Draft 2020-12 compliance.
	 * This property is retained for Draft 04 backward compatibility.
	 *
	 * @return
	 * 	The value of the <property>definitions</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema> getDefinitions() {
		return nn(definitions) ? definitions : defs; // Fall back to $defs for compatibility
	}

	/**
	 * Bean property setter:  <property>definitions</property>.
	 *
	 * @param definitions The new value for the <property>definitions</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setDefinitions(Map<String,JsonSchema> definitions) {
		this.definitions = definitions;
		if (nn(definitions))
			setMasterOn(definitions.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * @param name The key in the definitions map entry.
	 * @param definition The value in the definitions map entry.
	 * @return This object.
	 */
	public JsonSchema addDefinition(String name, JsonSchema definition) {
		if (this.definitions == null)
			this.definitions = map();
		this.definitions.put(name, definition);
		setMasterOn(definition);
		return this;
	}

	/**
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The value of the <property>properties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema> getProperties() { return properties; }

	/**
	 * Returns the property with the specified name.
	 *
	 * <p>
	 * This is equivalent to calling <property>getProperty(name, <jk>false</jk>)</property>.
	 *
	 * @param name The property name.
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public JsonSchema getProperty(String name) {
		return getProperty(name, false);
	}

	/**
	 * Returns the property with the specified name.
	 *
	 * <p>
	 * If <property>resolve</property> is <jk>true</jk>, the property object will automatically be  resolved by calling
	 * {@link #resolve()}.
	 * Therefore, <property>getProperty(name, <jk>true</jk>)</property> is equivalent to calling
	 * <property>getProperty(name).resolve()</property>, except it's safe from a potential
	 * <property>NullPointerException</property>.
	 *
	 * @param name The property name.
	 * @param resolve If <jk>true</jk>, calls {@link #resolve()} on object before returning.
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public JsonSchema getProperty(String name, boolean resolve) {
		if (properties == null)
			return null;
		var s = properties.get(name);
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
	 * @return This object.
	 */
	public JsonSchema setProperties(Map<String,JsonSchema> properties) {
		this.properties = properties;
		if (nn(properties)) {
			properties.entrySet().forEach(x -> {
				var value = x.getValue();
				setMasterOn(value);
				value.setName(x.getKey());
			});
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * <p>
	 * Properties must have their <property>name</property> property set on them when using this method.
	 *
	 * @param properties The list of items to append to the <property>properties</property> property on this bean.
	 * @return This object.
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public JsonSchema addProperties(JsonSchema...properties) {
		if (this.properties == null)
			this.properties = map();
		for (var p : properties) {
			if (p.getName() == null)
				throw bex(JsonSchema.class, "Invalid property passed to JsonSchema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.properties.put(p.getName(), p);
		}
		return this;
	}

	/**
	 * Bean property getter:  <property>patternProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>patternProperties</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Map<String,JsonSchema> getPatternProperties() { return patternProperties; }

	/**
	 * Bean property setter:  <property>patternProperties</property>.
	 *
	 * @param patternProperties The new value for the <property>patternProperties</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setPatternProperties(Map<String,JsonSchema> patternProperties) {
		this.patternProperties = patternProperties;
		if (nn(patternProperties)) {
			patternProperties.entrySet().forEach(x -> {
				var s = x.getValue();
				setMasterOn(s);
				s.setName(x.getKey());
			});
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>patternProperties</property>.
	 *
	 * <p>
	 * Properties must have their <property>name</property> property set to the pattern string when using this method.
	 *
	 * @param properties The list of items to append to the <property>patternProperties</property> property on this bean.
	 * @return This object.
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public JsonSchema addPatternProperties(JsonSchemaProperty...properties) {
		if (this.patternProperties == null)
			this.patternProperties = map();
		for (var p : properties) {
			if (p.getName() == null)
				throw bex(JsonSchema.class, "Invalid property passed to JsonSchema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.patternProperties.put(p.getName(), p);
		}
		return this;
	}

	/**
	 * Bean property getter:  <property>dependencies</property>.
	 *
	 * @return
	 * 	The value of the <property>dependencies</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema> getDependencies() { return dependencies; }

	/**
	 * Bean property setter:  <property>dependencies</property>.
	 *
	 * @param dependencies The new value for the <property>dependencies</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setDependencies(Map<String,JsonSchema> dependencies) {
		this.dependencies = dependencies;
		if (nn(dependencies))
			setMasterOn(dependencies.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>dependencies</property>.
	 *
	 * @param name The key of the entry in the dependencies map.
	 * @param dependency The value of the entry in the dependencies map.
	 * @return This object.
	 */
	public JsonSchema addDependency(String name, JsonSchema dependency) {
		if (this.dependencies == null)
			this.dependencies = map();
		this.dependencies.put(name, dependency);
		setMasterOn(dependency);
		return this;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * @return
	 * 	The value of the <property>items</property> property on this bean, or <jk>null</jk> if it is not set.
	 * 	Can be either a {@link JsonSchema} or {@link JsonSchemaArray} depending on what value was used to set it.
	 */
	@Swap(JsonSchemaOrSchemaArraySwap.class)
	public Object getItems() {
		if (nn(itemsSchema))
			return itemsSchema;
		return itemsSchemaArray;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>items</property> property when it is a {@link JsonSchema} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonSchemaArray}.
	 */
	@BeanIgnore
	public JsonSchema getItemsAsSchema() { return itemsSchema; }

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>items</property> property when it is a {@link JsonSchemaArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonSchema}.
	 */
	@BeanIgnore
	public JsonSchemaArray getItemsAsSchemaArray() { return itemsSchemaArray; }

	/**
	 * Used during parsing to convert the <property>items</property> property to the correct class type.
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If parsing a JSON-array, converts to a {@link JsonSchemaArray}.
	 * 	<li>
	 * 		If parsing a JSON-object, converts to a {@link JsonSchema}.
	 * </ul>
	 *
	 * <p>
	 * Serialization method is a no-op.
	 */
	public static class JsonSchemaOrSchemaArraySwap extends ObjectSwap<Object,Object> {

		@Override /* Overridden from ObjectSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonSchemaArray.class) : session.getClassMeta(JsonSchema.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * @param
	 * 	items The new value for the <property>items</property> property on this bean.
	 * 	This object must be of type {@link JsonSchema} or {@link JsonSchemaArray}.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public JsonSchema setItems(Object items) {
		this.itemsSchema = null;
		this.itemsSchemaArray = null;
		if (nn(items)) {
			if (items instanceof JsonSchema items2) {
				this.itemsSchema = items2;
				setMasterOn(this.itemsSchema);
			} else if (items instanceof JsonSchemaArray items2) {
				this.itemsSchemaArray = items2;
				setMasterOn(this.itemsSchemaArray);
			} else {
				throw bex(JsonSchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  JsonSchema, JsonSchemaArray", cn(items));
			}
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>items</property>.
	 *
	 * @param items The list of items to append to the <property>items</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addItems(JsonSchema...items) {
		if (this.itemsSchemaArray == null)
			this.itemsSchemaArray = new JsonSchemaArray();
		this.itemsSchemaArray.addAll(items);
		setMasterOn(items);
		return this;
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The value of the <property>multipleOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() { return multipleOf; }

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
		return this;
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The value of the <property>maximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() { return maximum; }

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMaximum(Number maximum) {
		this.maximum = maximum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive upper bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @return
	 * 	The value of the <property>exclusiveMaximum</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Number getExclusiveMaximum() { return exclusiveMaximum; }

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive upper bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setExclusiveMaximum(Number exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The value of the <property>minimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() { return minimum; }

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMinimum(Number minimum) {
		this.minimum = minimum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive lower bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @return
	 * 	The value of the <property>exclusiveMinimum</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Number getExclusiveMinimum() { return exclusiveMinimum; }

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive lower bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setExclusiveMinimum(Number exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The value of the <property>maxLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() { return maxLength; }

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The value of the <property>minLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() { return minLength; }

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMinLength(Integer minLength) {
		this.minLength = minLength;
		return this;
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The value of the <property>pattern</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() { return pattern; }

	/**
	 * Bean property setter:  <property>pattern</property>.
	 *
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 *
	 * @return
	 * 	The value of the <property>additionalItems</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 * 	Can be either a {@link Boolean} or {@link JsonSchemaArray} depending on what value was used to set it.
	 */
	@Swap(BooleanOrSchemaArraySwap.class)
	public Object getAdditionalItems() {
		if (nn(additionalItemsBoolean))
			return additionalItemsBoolean;
		return additionalItemsSchemaArray;
	}

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>additionalItems</property> property when it is a {@link Boolean}
	 * value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonSchemaArray}.
	 */
	@BeanIgnore
	public Boolean getAdditionalItemsAsBoolean() { return additionalItemsBoolean; }

	/**
	 * Bean property getter:  <property>additionalItems</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>additionalItems</property> property when it is a
	 * {@link JsonSchemaArray} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Boolean}.
	 */
	@BeanIgnore
	public List<JsonSchema> getAdditionalItemsAsSchemaArray() { return additionalItemsSchemaArray; }

	/**
	 * Bean property setter:  <property>additionalItems</property>.
	 *
	 * @param additionalItems
	 * 	The new value for the <property>additionalItems</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link JsonSchemaArray}.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public JsonSchema setAdditionalItems(Object additionalItems) {
		this.additionalItemsBoolean = null;
		this.additionalItemsSchemaArray = null;
		if (nn(additionalItems)) {
			if (additionalItems instanceof Boolean additionalItems2)
				this.additionalItemsBoolean = additionalItems2;
			else if (additionalItems instanceof JsonSchemaArray additionalItems2) {
				this.additionalItemsSchemaArray = additionalItems2;
				setMasterOn(this.additionalItemsSchemaArray);
			} else {
				throw bex(JsonSchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  Boolean, JsonSchemaArray", cn(additionalItems));
			}
		}
		return this;
	}

	/**
	 * Bean property appender:  <property>additionalItems</property>.
	 *
	 * @param additionalItems
	 * 	The list of items to append to the <property>additionalItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addAdditionalItems(JsonSchema...additionalItems) {
		if (this.additionalItemsSchemaArray == null)
			this.additionalItemsSchemaArray = new JsonSchemaArray();
		this.additionalItemsSchemaArray.addAll(additionalItems);
		setMasterOn(additionalItems);
		return this;
	}

	/**
	 * Used during parsing to convert the <property>additionalItems</property> property to the correct class type.
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If parsing a JSON-array, converts to a {@link JsonSchemaArray}.
	 * 	<li>
	 * 		If parsing a JSON-boolean, converts to a {@link Boolean}.
	 * </ul>
	 *
	 * <p>
	 * Serialization method is a no-op.
	 */
	public static class BooleanOrSchemaArraySwap extends ObjectSwap<Object,Object> {

		@Override /* Overridden from ObjectSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonSchemaArray.class) : session.getClassMeta(Boolean.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The value of the <property>maxItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() { return maxItems; }

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The value of the <property>minItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() { return minItems; }

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMinItems(Integer minItems) {
		this.minItems = minItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return
	 * 	The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() { return uniqueItems; }

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>maxProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxProperties() { return maxProperties; }

	/**
	 * Bean property setter:  <property>maxProperties</property>.
	 *
	 * @param maxProperties The new value for the <property>maxProperties</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMaxProperties(Integer maxProperties) {
		this.maxProperties = maxProperties;
		return this;
	}

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>minProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinProperties() { return minProperties; }

	/**
	 * Bean property setter:  <property>minProperties</property>.
	 *
	 * @param minProperties The new value for the <property>minProperties</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setMinProperties(Integer minProperties) {
		this.minProperties = minProperties;
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * @return The value of the <property>required</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getRequired() { return required; }

	/**
	 * Bean property setter:  <property>required</property>.
	 *
	 * @param required The new value for the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setRequired(List<String> required) {
		this.required = required;
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param required The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addRequired(List<String> required) {
		if (this.required == null)
			this.required = new LinkedList<>();
		required.forEach(x -> this.required.add(x));
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param required The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addRequired(String...required) {
		if (this.required == null)
			this.required = new LinkedList<>();
		for (var r : required)
			this.required.add(r);
		return this;
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param properties The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addRequired(JsonSchemaProperty...properties) {
		if (this.required == null)
			this.required = new LinkedList<>();
		for (var p : properties)
			this.required.add(p.getName());
		return this;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>additionalProperties</property> property on this bean, or <jk>null</jk> if it
	 * 	is not set.
	 * 	Can be either a {@link Boolean} or {@link JsonSchemaArray} depending on what value was used to set it.
	 */
	@Swap(BooleanOrSchemaSwap.class)
	public Object getAdditionalProperties() {
		if (nn(additionalPropertiesBoolean))
			return additionalItemsBoolean;
		return additionalPropertiesSchema;
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>additionalProperties</property> property when it is a
	 * {@link Boolean} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link JsonSchema}.
	 */
	@BeanIgnore
	public Boolean getAdditionalPropertiesAsBoolean() { return additionalPropertiesBoolean; }

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * <p>
	 * Convenience method for returning the <property>additionalProperties</property> property when it is a
	 * {@link JsonSchema} value.
	 *
	 * @return The currently set value, or <jk>null</jk> if the property is not set, or is set as a {@link Boolean}.
	 */
	@BeanIgnore
	public JsonSchema getAdditionalPropertiesAsSchema() { return additionalPropertiesSchema; }

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param additionalProperties
	 * 	The new value for the <property>additionalProperties</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link JsonSchema}.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	@Beanp(dictionary = { JsonSchema.class })
	public JsonSchema setAdditionalProperties(Object additionalProperties) {
		this.additionalPropertiesBoolean = null;
		this.additionalPropertiesSchema = null;
		if (nn(additionalProperties)) {
			if (additionalProperties instanceof Boolean additionalProperties2)
				this.additionalPropertiesBoolean = additionalProperties2;
			else if (additionalProperties instanceof JsonSchema additionalProperties2) {
				this.additionalPropertiesSchema = additionalProperties2;
				setMasterOn(this.additionalPropertiesSchema);
			} else
				throw bex(JsonSchemaProperty.class, "Invalid attribute type ''{0}'' passed in.  Must be one of the following:  Boolean, JsonSchema", cn(additionalProperties));
		}
		return this;
	}

	/**
	 * Used during parsing to convert the <property>additionalProperties</property> property to the correct class type.
	 *
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If parsing a JSON-object, converts to a {@link JsonSchema}.
	 * 	<li>
	 * 		If parsing a JSON-boolean, converts to a {@link Boolean}.
	 * </ul>
	 *
	 * <p>
	 * Serialization method is a no-op.
	 */
	public static class BooleanOrSchemaSwap extends ObjectSwap<Object,Object> {

		@Override /* Overridden from ObjectSwap */
		public Object swap(BeanSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(BeanSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Boolean ? session.getClassMeta(Boolean.class) : session.getClassMeta(JsonSchema.class));
			return session.convertToType(o, cm);
		}
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() { return _enum; }

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setEnum(List<Object> _enum) { // NOSONAR - Intentional naming.
		this._enum = _enum;
		return this;
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param _enum The list of items to append to the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addEnum(Object..._enum) { // NOSONAR - Intentional naming.
		if (this._enum == null)
			this._enum = new LinkedList<>();
		for (var e : _enum)
			this._enum.add(e);
		return this;
	}

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The value of the <property>allOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema> getAllOf() { return allOf; }

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param allOf The new value for the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setAllOf(List<JsonSchema> allOf) {
		this.allOf = allOf;
		setMasterOn(allOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param allOf The list of items to append to the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addAllOf(JsonSchema...allOf) {
		setMasterOn(allOf);
		this.allOf = addAll(this.allOf, allOf);
		return this;
	}

	/**
	 * Bean property getter:  <property>anyOf</property>.
	 *
	 * @return The value of the <property>anyOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema> getAnyOf() { return anyOf; }

	/**
	 * Bean property setter:  <property>anyOf</property>.
	 *
	 * @param anyOf The new value of the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setAnyOf(List<JsonSchema> anyOf) {
		this.anyOf = anyOf;
		setMasterOn(anyOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>anyOf</property>.
	 *
	 * @param anyOf The list of items to append to the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addAnyOf(JsonSchema...anyOf) {
		if (this.anyOf == null)
			this.anyOf = new LinkedList<>();
		setMasterOn(anyOf);
		for (var s : anyOf)
			this.anyOf.add(s);
		return this;
	}

	/**
	 * Bean property getter:  <property>oneOf</property>.
	 *
	 * @return The value of the <property>oneOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema> getOneOf() { return oneOf; }

	/**
	 * Bean property setter:  <property>oneOf</property>.
	 *
	 * @param oneOf The new value for the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setOneOf(List<JsonSchema> oneOf) {
		this.oneOf = oneOf;
		setMasterOn(oneOf);
		return this;
	}

	/**
	 * Bean property appender:  <property>oneOf</property>.
	 *
	 * @param oneOf The list of items to append to the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addOneOf(JsonSchema...oneOf) {
		if (this.oneOf == null)
			this.oneOf = new LinkedList<>();
		setMasterOn(oneOf);
		for (var s : oneOf)
			this.oneOf.add(s);
		return this;
	}

	/**
	 * Bean property getter:  <property>not</property>.
	 *
	 * @return The value of the <property>not</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema getNot() { return not; }

	/**
	 * Bean property setter:  <property>not</property>.
	 *
	 * @param not The new value for the <property>not</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setNot(JsonSchema not) {
		this.not = not;
		setMasterOn(not);
		return this;
	}

	/**
	 * Bean property getter:  <property>const</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @return The value of the <property>const</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getConst() { return _const; }

	/**
	 * Bean property setter:  <property>const</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @param _const The new value for the <property>const</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setConst(Object _const) { // NOSONAR - Intentional naming.
		this._const = _const;
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @return The value of the <property>examples</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getExamples() { return examples; }

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @param examples The new value for the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setExamples(List<Object> examples) {
		this.examples = examples;
		return this;
	}

	/**
	 * Bean property appender:  <property>examples</property>.
	 *
	 * @param examples The list of items to append to the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addExamples(Object...examples) {
		if (this.examples == null)
			this.examples = new LinkedList<>();
		for (var e : examples)
			this.examples.add(e);
		return this;
	}

	/**
	 * Bean property getter:  <property>if</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>if</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("if")
	public JsonSchema getIf() { return _if; }

	/**
	 * Bean property setter:  <property>if</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param _if The new value for the <property>if</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("if")
	public JsonSchema setIf(JsonSchema _if) { // NOSONAR - Intentional naming.
		this._if = _if;
		setMasterOn(_if);
		return this;
	}

	/**
	 * Bean property getter:  <property>then</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>then</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("then")
	public JsonSchema getThen() { return _then; }

	/**
	 * Bean property setter:  <property>then</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param _then The new value for the <property>then</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("then")
	public JsonSchema setThen(JsonSchema _then) { // NOSONAR - Intentional naming.
		this._then = _then;
		setMasterOn(_then);
		return this;
	}

	/**
	 * Bean property getter:  <property>else</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>else</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("else")
	public JsonSchema getElse() { return _else; }

	/**
	 * Bean property setter:  <property>else</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param _else The new value for the <property>else</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("else")
	public JsonSchema setElse(JsonSchema _else) { // NOSONAR - Intentional naming.
		this._else = _else;
		setMasterOn(_else);
		return this;
	}

	/**
	 * Bean property getter:  <property>readOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>readOnly</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getReadOnly() { return readOnly; }

	/**
	 * Bean property setter:  <property>readOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param readOnly The new value for the <property>readOnly</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	/**
	 * Bean property getter:  <property>writeOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>writeOnly</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getWriteOnly() { return writeOnly; }

	/**
	 * Bean property setter:  <property>writeOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param writeOnly The new value for the <property>writeOnly</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setWriteOnly(Boolean writeOnly) {
		this.writeOnly = writeOnly;
		return this;
	}

	/**
	 * Bean property getter:  <property>contentMediaType</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>contentMediaType</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getContentMediaType() { return contentMediaType; }

	/**
	 * Bean property setter:  <property>contentMediaType</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param contentMediaType The new value for the <property>contentMediaType</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setContentMediaType(String contentMediaType) {
		this.contentMediaType = contentMediaType;
		return this;
	}

	/**
	 * Bean property getter:  <property>contentEncoding</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>contentEncoding</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getContentEncoding() { return contentEncoding; }

	/**
	 * Bean property setter:  <property>contentEncoding</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param contentEncoding The new value for the <property>contentEncoding</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
		return this;
	}

	/**
	 * Bean property getter:  <property>$defs</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 replacement for <property>definitions</property>.
	 * Both properties are supported for backward compatibility.
	 *
	 * @return The value of the <property>$defs</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$defs")
	public Map<String,JsonSchema> getDefs() {
		return defs; // Return only defs, not definitions (to avoid double serialization)
	}

	/**
	 * Bean property setter:  <property>$defs</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 replacement for <property>definitions</property>.
	 * Both properties are supported for backward compatibility.
	 *
	 * @param defs The new value for the <property>$defs</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("$defs")
	public JsonSchema setDefs(Map<String,JsonSchema> defs) {
		this.defs = defs;
		if (nn(defs))
			setMasterOn(defs.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>$defs</property>.
	 *
	 * @param name The key in the defs map entry.
	 * @param def The value in the defs map entry.
	 * @return This object.
	 */
	public JsonSchema addDef(String name, JsonSchema def) {
		if (this.defs == null)
			this.defs = map();
		this.defs.put(name, def);
		setMasterOn(def);
		return this;
	}

	/**
	 * Bean property getter:  <property>prefixItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2020-12 for tuple validation.
	 *
	 * @return The value of the <property>prefixItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchemaArray getPrefixItems() { return prefixItems; }

	/**
	 * Bean property setter:  <property>prefixItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2020-12 for tuple validation.
	 *
	 * @param prefixItems The new value for the <property>prefixItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setPrefixItems(JsonSchemaArray prefixItems) {
		this.prefixItems = prefixItems;
		setMasterOn(prefixItems);
		return this;
	}

	/**
	 * Bean property appender:  <property>prefixItems</property>.
	 *
	 * @param prefixItems The list of items to append to the <property>prefixItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema addPrefixItems(JsonSchema...prefixItems) {
		if (this.prefixItems == null)
			this.prefixItems = new JsonSchemaArray();
		this.prefixItems.addAll(prefixItems);
		setMasterOn(prefixItems);
		return this;
	}

	/**
	 * Bean property getter:  <property>unevaluatedItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @return The value of the <property>unevaluatedItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema getUnevaluatedItems() { return unevaluatedItems; }

	/**
	 * Bean property setter:  <property>unevaluatedItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @param unevaluatedItems The new value for the <property>unevaluatedItems</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setUnevaluatedItems(JsonSchema unevaluatedItems) {
		this.unevaluatedItems = unevaluatedItems;
		setMasterOn(unevaluatedItems);
		return this;
	}

	/**
	 * Bean property getter:  <property>unevaluatedProperties</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @return The value of the <property>unevaluatedProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema getUnevaluatedProperties() { return unevaluatedProperties; }

	/**
	 * Bean property setter:  <property>unevaluatedProperties</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @param unevaluatedProperties The new value for the <property>unevaluatedProperties</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setUnevaluatedProperties(JsonSchema unevaluatedProperties) {
		this.unevaluatedProperties = unevaluatedProperties;
		setMasterOn(unevaluatedProperties);
		return this;
	}

	/**
	 * Bean property getter:  <property>dependentSchemas</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the schema form of <property>dependencies</property>.
	 *
	 * @return The value of the <property>dependentSchemas</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema> getDependentSchemas() { return dependentSchemas; }

	/**
	 * Bean property setter:  <property>dependentSchemas</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the schema form of <property>dependencies</property>.
	 *
	 * @param dependentSchemas The new value for the <property>dependentSchemas</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setDependentSchemas(Map<String,JsonSchema> dependentSchemas) {
		this.dependentSchemas = dependentSchemas;
		if (nn(dependentSchemas))
			setMasterOn(dependentSchemas.values());
		return this;
	}

	/**
	 * Bean property appender:  <property>dependentSchemas</property>.
	 *
	 * @param name The key of the entry in the dependentSchemas map.
	 * @param schema The value of the entry in the dependentSchemas map.
	 * @return This object.
	 */
	public JsonSchema addDependentSchema(String name, JsonSchema schema) {
		if (this.dependentSchemas == null)
			this.dependentSchemas = map();
		this.dependentSchemas.put(name, schema);
		setMasterOn(schema);
		return this;
	}

	/**
	 * Bean property getter:  <property>dependentRequired</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the array form of <property>dependencies</property>.
	 *
	 * @return The value of the <property>dependentRequired</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,List<String>> getDependentRequired() { return dependentRequired; }

	/**
	 * Bean property setter:  <property>dependentRequired</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the array form of <property>dependencies</property>.
	 *
	 * @param dependentRequired The new value for the <property>dependentRequired</property> property on this bean.
	 * @return This object.
	 */
	public JsonSchema setDependentRequired(Map<String,List<String>> dependentRequired) {
		this.dependentRequired = dependentRequired;
		return this;
	}

	/**
	 * Bean property appender:  <property>dependentRequired</property>.
	 *
	 * @param name The key of the entry in the dependentRequired map.
	 * @param required The value of the entry in the dependentRequired map.
	 * @return This object.
	 */
	public JsonSchema addDependentRequired(String name, List<String> required) {
		if (this.dependentRequired == null)
			this.dependentRequired = map();
		this.dependentRequired.put(name, required);
		return this;
	}

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The value of the <property>$ref</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public URI getRef() { return ref; }

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param ref The new value for the <property>$ref</property> property on this bean.
	 * @return This object.
	 */
	@Beanp("$ref")
	public JsonSchema setRef(Object ref) {
		this.ref = toURI(ref);
		return this;
	}

	private void setMasterOn(JsonSchema s) {
		if (nn(s))
			s.setMaster(this);
	}

	private void setMasterOn(JsonSchema[] ss) {
		if (nn(ss))
			for (var s : ss)
				setMasterOn(s);
	}

	private void setMasterOn(Collection<JsonSchema> ss) {
		if (nn(ss))
			ss.forEach(this::setMasterOn);
	}

	private void setMasterOn(JsonSchemaArray ss) {
		if (nn(ss))
			ss.forEach(this::setMasterOn);
	}

	/**
	 * Sets the master schema for this schema and all child schema objects.
	 *
	 * <p>
	 * All child elements in a schema should point to a single "master" schema in order to locate registered JsonSchemaMap
	 * objects for resolving external schemas.
	 *
	 * @param master The master schema to associate on this and all children.  Can be <jk>null</jk>.
	 */
	protected void setMaster(JsonSchema master) {
		this.master = master;
		if (nn(definitions))
			definitions.values().forEach(x -> x.setMaster(master));
		if (nn(defs))
			defs.values().forEach(x -> x.setMaster(master));
		if (nn(properties))
			properties.values().forEach(x -> x.setMaster(master));
		if (nn(patternProperties))
			patternProperties.values().forEach(x -> x.setMaster(master));
		if (nn(dependencies))
			dependencies.values().forEach(x -> x.setMaster(master));
		if (nn(dependentSchemas))
			dependentSchemas.values().forEach(x -> x.setMaster(master));
		if (nn(itemsSchema))
			itemsSchema.setMaster(master);
		if (nn(itemsSchemaArray))
			itemsSchemaArray.forEach(x -> x.setMaster(master));
		if (nn(prefixItems))
			prefixItems.forEach(x -> x.setMaster(master));
		if (nn(additionalItemsSchemaArray))
			additionalItemsSchemaArray.forEach(x -> x.setMaster(master));
		if (nn(unevaluatedItems))
			unevaluatedItems.setMaster(master);
		if (nn(additionalPropertiesSchema))
			additionalPropertiesSchema.setMaster(master);
		if (nn(unevaluatedProperties))
			unevaluatedProperties.setMaster(master);
		if (nn(allOf))
			allOf.forEach(x -> x.setMaster(master));
		if (nn(anyOf))
			anyOf.forEach(x -> x.setMaster(master));
		if (nn(oneOf))
			oneOf.forEach(x -> x.setMaster(master));
		if (nn(not))
			not.setMaster(master);
		if (nn(_if))
			_if.setMaster(master);
		if (nn(_then))
			_then.setMaster(master);
		if (nn(_else))
			_else.setMaster(master);
	}

	/**
	 * Resolve schema if reference.
	 *
	 * <p>
	 * If this schema is a reference to another schema (has its <property>$ref</property> property set), this
	 * method will retrieve the referenced schema from the schema map registered with this schema.
	 *
	 * <p>
	 * If this schema is not a reference, or no schema map is registered with this schema, this method is a no-op and
	 * simply returns this object.
	 *
	 * @return The referenced schema, or <jk>null</jk>.
	 */
	public JsonSchema resolve() {
		if (ref == null || master.schemaMap == null)
			return this;
		return master.schemaMap.get(ref);
	}

	/**
	 * Associates a schema map with this schema for resolving other schemas identified through <property>$ref</property>
	 * properties.
	 *
	 * @param schemaMap The schema map to associate with this schema.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@BeanIgnore
	public JsonSchema setSchemaMap(JsonSchemaMap schemaMap) {
		this.schemaMap = schemaMap;
		return this;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return JsonSerializer.DEFAULT_SORTED.toString(this);
	}
}