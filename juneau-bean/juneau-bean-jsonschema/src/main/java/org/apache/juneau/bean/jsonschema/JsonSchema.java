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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

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
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.write(<jv>schema</jv>);
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
 * 	<li><b>Auto Generation:</b> Use {@link JsonSchemaBeanGenerator} to generate schemas from Java types
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
 *
 * @param <SELF> The self type for fluent setters.
 */
@Marshalled(typeName = "schema")
@SuppressWarnings({
	"java:S116", // Field names follow OpenAPI/JSON Schema spec
	"java:S119", // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	"java:S6539" // Monster class; JsonSchema intentionally models the entire JSON Schema Draft 2020-12 keyword set as one cohesive bean
})
public class JsonSchema<SELF extends JsonSchema<SELF>> {

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
		public Object swap(MarshallingSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(MarshallingSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonSchemaArray.class) : session.getClassMeta(Boolean.class));
			return session.convertToType(o, cm);
		}
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
		public Object swap(MarshallingSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(MarshallingSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Boolean ? session.getClassMeta(Boolean.class) : session.getClassMeta(JsonSchema.class));
			return session.convertToType(o, cm);
		}
	}

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
		public Object swap(MarshallingSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(MarshallingSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonSchemaArray.class) : session.getClassMeta(JsonSchema.class));
			return session.convertToType(o, cm);
		}
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
		public Object swap(MarshallingSession session, Object o) throws SerializeException {
			return o;
		}

		@Override /* Overridden from ObjectSwap */
		public Object unswap(MarshallingSession session, Object o, ClassMeta<?> hint) throws ParseException {
			var cm = (o instanceof Collection ? session.getClassMeta(JsonTypeArray.class) : session.getClassMeta(JsonType.class));
			return session.convertToType(o, cm);
		}
	}

	// @formatter:off
	private String name;                                   // Property name.  Not serialized.
	private URI idUri;                                     // Draft 2020-12: $id
	private URI id;                                        // Draft 04: id (deprecated but kept for compatibility)
	private URI schemaVersion;
	private String title;
	private String summary;
	private String description;
	private JsonType typeJsonType;                         // JsonType representation of type
	private JsonTypeArray typeJsonTypeArray;               // JsonTypeArray representation of type
	private Map<String,JsonSchema<?>> definitions;            // Retained for backward compatibility
	private Map<String,JsonSchema<?>> defs;                   // Draft 2020-12: $defs
	private Map<String,JsonSchema<?>> properties;
	private Map<String,JsonSchema<?>> patternProperties;
	private Map<String,JsonSchema<?>> dependencies;           // Retained for backward compatibility
	private Map<String,JsonSchema<?>> dependentSchemas;       // Draft 2019-09+
	private Map<String,List<String>> dependentRequired;    // Draft 2019-09+
	private JsonSchema<?> itemsSchema;                        // JsonSchema representation of items
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
	private JsonSchema<?> unevaluatedItems;                   // Draft 2019-09+
	private Integer maxItems;
	private Integer minItems;
	private Boolean uniqueItems;
	private Integer maxProperties;
	private Integer minProperties;
	private List<String> required;
	private Boolean additionalPropertiesBoolean;           // Boolean representation of additionalProperties
	private JsonSchema<?> additionalPropertiesSchema;         // JsonSchema representation of additionalProperties
	private JsonSchema<?> unevaluatedProperties;              // Draft 2019-09+
	private List<Object> enum_;                            // Changed to Object to support any type
	private Object const_;                                 // Draft 06+
	private List<Object> examples;                         // Draft 06+
	private List<JsonSchema<?>> allOf;
	private List<JsonSchema<?>> anyOf;
	private List<JsonSchema<?>> oneOf;
	private JsonSchema<?> not;
	private JsonSchema<?> if_;                                // Draft 07+
	private JsonSchema<?> then_;                              // Draft 07+
	private JsonSchema<?> else_;                              // Draft 07+
	private Boolean readOnly;                              // Draft 07+
	private Boolean writeOnly;                             // Draft 07+
	private String format;                                 // Annotation-level keyword
	private String comment;                                // Draft 07+, serialized as $comment
	private Boolean deprecated;                            // Draft 2019-09+
	private String contentMediaType;                       // Draft 07+

	private String contentEncoding;                        // Draft 07+

	private URI ref;

	private JsonSchemaMap schemaMap;

	private JsonSchema<?> master = this;
	// @formatter:on

	/**
	 * Default constructor.
	 */
	public JsonSchema() { /* Empty constructor. */ }

	@SuppressWarnings("unchecked")
	private SELF self() {
		return (SELF) this;
	}

	/**
	 * Generates a {@link JsonSchema} bean from the specified type using {@link JsonSchemaBeanGenerator#DEFAULT}.
	 *
	 * @param type The type to generate a schema for.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The generated schema bean, or <jk>null</jk> if a schema could not be generated for the type.
	 */
	public static JsonSchema<?> of(Type type) {
		return JsonSchemaBeanGenerator.DEFAULT.generate(type);
	}

	/**
	 * Generates a {@link JsonSchema} bean from the specified class using {@link JsonSchemaBeanGenerator#DEFAULT}.
	 *
	 * @param type The class to generate a schema for.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return The generated schema bean, or <jk>null</jk> if a schema could not be generated for the class.
	 */
	public static JsonSchema<?> of(Class<?> type) {
		return JsonSchemaBeanGenerator.DEFAULT.generate(type);
	}

	/**
	 * Bean property appender:  <property>additionalItems</property>.
	 *
	 * @param value
	 * 	The list of items to append to the <property>additionalItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF addAdditionalItems(JsonSchema<?>...value) {
		if (this.additionalItemsSchemaArray == null)
			this.additionalItemsSchemaArray = new JsonSchemaArray();
		this.additionalItemsSchemaArray.addAll(value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param value The list of items to append to the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addAllOf(JsonSchema<?>...value) {
		setMasterOn(value);
		this.allOf = addAll(this.allOf, value);
		return self();
	}

	/**
	 * Bean property appender:  <property>allOf</property>.
	 *
	 * @param value The collection of items to append to the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addAllOf(Collection<JsonSchema<?>> value) {
		setMasterOn(value);
		this.allOf = addAll(this.allOf, value == null ? null : new ArrayList<>(value));
		return self();
	}

	/**
	 * Bean property appender:  <property>anyOf</property>.
	 *
	 * @param value The list of items to append to the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addAnyOf(JsonSchema<?>...value) {
		if (this.anyOf == null)
			this.anyOf = new LinkedList<>();
		setMasterOn(value);
		for (var s : value)
			this.anyOf.add(s);
		return self();
	}

	/**
	 * Bean property appender:  <property>anyOf</property>.
	 *
	 * @param value The collection of items to append to the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addAnyOf(Collection<JsonSchema<?>> value) {
		if (this.anyOf == null)
			this.anyOf = new LinkedList<>();
		setMasterOn(value);
		this.anyOf.addAll(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>$defs</property>.
	 *
	 * @param name The key in the defs map entry.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value in the defs map entry.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addDef(String name, JsonSchema<?> value) {
		if (this.defs == null)
			this.defs = map();
		this.defs.put(name, value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>definitions</property>.
	 *
	 * @param name The key in the definitions map entry.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value in the definitions map entry.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addDefinition(String name, JsonSchema<?> value) {
		if (this.definitions == null)
			this.definitions = map();
		this.definitions.put(name, value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>dependencies</property>.
	 *
	 * @param name The key of the entry in the dependencies map.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value of the entry in the dependencies map.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addDependency(String name, JsonSchema<?> value) {
		if (this.dependencies == null)
			this.dependencies = map();
		this.dependencies.put(name, value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>dependentRequired</property>.
	 *
	 * @param name The key of the entry in the dependentRequired map.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value of the entry in the dependentRequired map.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addDependentRequired(String name, List<String> value) {
		if (this.dependentRequired == null)
			this.dependentRequired = map();
		this.dependentRequired.put(name, value);
		return self();
	}

	/**
	 * Bean property appender:  <property>dependentSchemas</property>.
	 *
	 * @param name The key of the entry in the dependentSchemas map.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value of the entry in the dependentSchemas map.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addDependentSchema(String name, JsonSchema<?> value) {
		if (this.dependentSchemas == null)
			this.dependentSchemas = map();
		this.dependentSchemas.put(name, value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param value The list of items to append to the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public SELF addEnum(Object...value) {
		if (this.enum_ == null)
			this.enum_ = new LinkedList<>();
		for (var e : value)
			this.enum_.add(e);
		return self();
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param value The collection of items to append to the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public SELF addEnum(Collection<Object> value) {
		if (this.enum_ == null)
			this.enum_ = new LinkedList<>();
		this.enum_.addAll(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>examples</property>.
	 *
	 * @param value The list of items to append to the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public SELF addExamples(Object...value) {
		if (this.examples == null)
			this.examples = new LinkedList<>();
		for (var e : value)
			this.examples.add(e);
		return self();
	}

	/**
	 * Bean property appender:  <property>examples</property>.
	 *
	 * @param value The collection of items to append to the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public SELF addExamples(Collection<Object> value) {
		if (this.examples == null)
			this.examples = new LinkedList<>();
		this.examples.addAll(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>items</property>.
	 *
	 * @param value The list of items to append to the <property>items</property> property on this bean.
	 * @return This object.
	 */
	public SELF addItems(JsonSchema<?>...value) {
		if (this.itemsSchemaArray == null)
			this.itemsSchemaArray = new JsonSchemaArray();
		this.itemsSchemaArray.addAll(value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>oneOf</property>.
	 *
	 * @param value The list of items to append to the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addOneOf(JsonSchema<?>...value) {
		if (this.oneOf == null)
			this.oneOf = new LinkedList<>();
		setMasterOn(value);
		for (var s : value)
			this.oneOf.add(s);
		return self();
	}

	/**
	 * Bean property appender:  <property>oneOf</property>.
	 *
	 * @param value The collection of items to append to the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF addOneOf(Collection<JsonSchema<?>> value) {
		if (this.oneOf == null)
			this.oneOf = new LinkedList<>();
		setMasterOn(value);
		this.oneOf.addAll(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>patternProperties</property>.
	 *
	 * <p>
	 * Properties must have their <property>name</property> property set to the pattern string when using this method.
	 *
	 * @param value The list of items to append to the <property>patternProperties</property> property on this bean.
	 * @return This object.
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public SELF addPatternProperties(JsonSchemaProperty<?>...value) {
		if (this.patternProperties == null)
			this.patternProperties = map();
		for (var p : value) {
			if (p.getName() == null)
				throw brex(JsonSchema.class, "Invalid property passed to JsonSchema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.patternProperties.put(p.getName(), p);
		}
		return self();
	}

	/**
	 * Bean property appender:  <property>patternProperties</property>.
	 *
	 * <p>
	 * Unlike {@link #addPatternProperties(JsonSchemaProperty...)}, this keyed form does not require the value's
	 * <property>name</property> property to be pre-set — it is set automatically from {@code name}.
	 *
	 * @param name The key in the patternProperties map entry.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value in the patternProperties map entry.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addPatternProperty(String name, JsonSchema<?> value) {
		if (this.patternProperties == null)
			this.patternProperties = map();
		if (value != null) {
			setMasterOn(value);
			value.setName(name);
		}
		this.patternProperties.put(name, value);
		return self();
	}

	/**
	 * Bean property appender:  <property>prefixItems</property>.
	 *
	 * @param value The list of items to append to the <property>prefixItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF addPrefixItems(JsonSchema<?>...value) {
		if (this.prefixItems == null)
			this.prefixItems = new JsonSchemaArray();
		this.prefixItems.addAll(value);
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * <p>
	 * Properties must have their <property>name</property> property set on them when using this method.
	 *
	 * @param value The list of items to append to the <property>properties</property> property on this bean.
	 * @return This object.
	 * @throws BeanRuntimeException If property is found without a set <property>name</property> property.
	 */
	public SELF addProperties(JsonSchema<?>...value) {
		if (this.properties == null)
			this.properties = map();
		for (var p : value) {
			if (p.getName() == null)
				throw brex(JsonSchema.class, "Invalid property passed to JsonSchema.addProperties().  Property name was null.");
			setMasterOn(p);
			this.properties.put(p.getName(), p);
		}
		return self();
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * <p>
	 * Unlike {@link #addProperties(JsonSchema...)}, this keyed form does not require the value's
	 * <property>name</property> property to be pre-set — it is set automatically from {@code name}.
	 *
	 * @param name The key in the properties map entry.  Can be <jk>null</jk> ({@link LinkedHashMap} tolerates a <jk>null</jk> key).
	 * @param value The value in the properties map entry.  Can be <jk>null</jk> (stored as <jk>null</jk>).
	 * @return This object.
	 */
	public SELF addProperty(String name, JsonSchema<?> value) {
		if (this.properties == null)
			this.properties = map();
		if (value != null) {
			setMasterOn(value);
			value.setName(name);
		}
		this.properties.put(name, value);
		return self();
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param value The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public SELF addRequired(JsonSchemaProperty<?>...value) {
		if (this.required == null)
			this.required = new LinkedList<>();
		for (var p : value)
			this.required.add(p.getName());
		return self();
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param value The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public SELF addRequired(List<String> value) {
		if (this.required == null)
			this.required = new LinkedList<>();
		value.forEach(x -> this.required.add(x));
		return self();
	}

	/**
	 * Bean property appender:  <property>required</property>.
	 *
	 * @param value The list of items to append to the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public SELF addRequired(String...value) {
		if (this.required == null)
			this.required = new LinkedList<>();
		for (var r : value)
			this.required.add(r);
		return self();
	}

	/**
	 * Bean property appender:  <property>type</property>.
	 *
	 * @param value The list of items to append to the <property>type</property> property on this bean.
	 * @return This object.
	 */
	public SELF addTypes(JsonType...value) {
		if (this.typeJsonTypeArray == null)
			this.typeJsonTypeArray = new JsonTypeArray();
		this.typeJsonTypeArray.addAll(value);
		return self();
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
	public List<JsonSchema<?>> getAdditionalItemsAsSchemaArray() { return u(additionalItemsSchemaArray); }

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>additionalProperties</property> property on this bean, or <jk>null</jk> if it
	 * 	is not set.
	 * 	Can be either a {@link Boolean} or {@link JsonSchema} depending on what value was used to set it.
	 */
	@Swap(BooleanOrSchemaSwap.class)
	public Object getAdditionalProperties() {
		if (nn(additionalPropertiesBoolean))
			return additionalPropertiesBoolean;
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
	public JsonSchema<?> getAdditionalPropertiesAsSchema() { return additionalPropertiesSchema; }

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The value of the <property>allOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema<?>> getAllOf() { return u(allOf); }

	/**
	 * Bean property getter:  <property>anyOf</property>.
	 *
	 * @return The value of the <property>anyOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema<?>> getAnyOf() { return u(anyOf); }

	/**
	 * Bean property getter:  <property>const</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @return The value of the <property>const</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getConst() { return const_; }

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
	 * Bean property getter:  <property>contentMediaType</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>contentMediaType</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getContentMediaType() { return contentMediaType; }

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @return The value of the <property>deprecated</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() { return deprecated; }

	/**
	 * Bean property getter:  <property>$comment</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>$comment</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("$comment")
	public String getComment() { return comment; }

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
	public Map<String,JsonSchema<?>> getDefinitions() {
		return nn(definitions) ? definitions : defs; // Fall back to $defs for compatibility
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
	@BeanProp("$defs")
	public Map<String,JsonSchema<?>> getDefs() {
		return defs; // Return only defs, not definitions (to avoid double serialization)
	}

	/**
	 * Bean property getter:  <property>dependencies</property>.
	 *
	 * @return
	 * 	The value of the <property>dependencies</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema<?>> getDependencies() { return u(dependencies); }

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * @return The value of the <property>format</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() { return format; }

	/**
	 * Bean property getter:  <property>dependentRequired</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the array form of <property>dependencies</property>.
	 *
	 * @return The value of the <property>dependentRequired</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,List<String>> getDependentRequired() { return u(dependentRequired); }

	/**
	 * Bean property getter:  <property>dependentSchemas</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the schema form of <property>dependencies</property>.
	 *
	 * @return The value of the <property>dependentSchemas</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema<?>> getDependentSchemas() { return u(dependentSchemas); }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The value of the <property>description</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>else</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>else</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("else")
	public JsonSchema<?> getElse() { return else_; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() { return u(enum_); }

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @return The value of the <property>examples</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getExamples() { return u(examples); }

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
	 * Bean property getter:  <property>id</property>.
	 *
	 * <p>
	 * <b>Deprecated:</b> Use {@link #getIdUri()} instead.
	 * This property is retained for Draft 04 backward compatibility.
	 *
	 * @return The value of the <property>id</property> property on this bean, or <jk>null</jk> if it is not set.
	 * @deprecated Use {@link #getIdUri()} instead.
	 */
	@Deprecated(since = "10.0", forRemoval = true)
	@SuppressWarnings({
		"java:S1133" // Kept for Draft 04 backward compatibility, will be removed in future version
	})
	public URI getId() {
		return nn(id) ? id : idUri; // Fall back to new '$id' for compatibility when reading
	}

	/**
	 * Bean property getter:  <property>$id</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 property for schema identification.
	 *
	 * @return The value of the <property>$id</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("$id")
	public URI getIdUri() {
		return idUri; // Return only idUri, not id (to avoid double serialization)
	}

	/**
	 * Bean property getter:  <property>if</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>if</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("if")
	public JsonSchema<?> getIf() { return if_; }

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
	public JsonSchema<?> getItemsAsSchema() { return itemsSchema; }

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
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The value of the <property>maximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() { return maximum; }

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The value of the <property>maxItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() { return maxItems; }

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The value of the <property>maxLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() { return maxLength; }

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>maxProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxProperties() { return maxProperties; }

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The value of the <property>minimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() { return minimum; }

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The value of the <property>minItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() { return minItems; }

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The value of the <property>minLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() { return minLength; }

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>minProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinProperties() { return minProperties; }

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The value of the <property>multipleOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() { return multipleOf; }

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
	 * Bean property getter:  <property>not</property>.
	 *
	 * @return The value of the <property>not</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema<?> getNot() { return not; }

	/**
	 * Bean property getter:  <property>oneOf</property>.
	 *
	 * @return The value of the <property>oneOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<JsonSchema<?>> getOneOf() { return u(oneOf); }

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The value of the <property>pattern</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() { return pattern; }

	/**
	 * Bean property getter:  <property>patternProperties</property>.
	 *
	 * @return
	 * 	The value of the <property>patternProperties</property> property on this bean, or <jk>null</jk> if it is
	 * 	not set.
	 */
	public Map<String,JsonSchema<?>> getPatternProperties() { return u(patternProperties); }

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
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The value of the <property>properties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,JsonSchema<?>> getProperties() { return u(properties); }

	/**
	 * Returns the property with the specified name.
	 *
	 * <p>
	 * This is equivalent to calling <property>getProperty(name, <jk>false</jk>)</property>.
	 *
	 * @param name The property name.  Can be <jk>null</jk> (no property has a <jk>null</jk> name, so <jk>null</jk> is returned).
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public JsonSchema<?> getProperty(String name) {
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
	 * @param name The property name.  Can be <jk>null</jk> (no property has a <jk>null</jk> name, so <jk>null</jk> is returned).
	 * @param resolve If <jk>true</jk>, calls {@link #resolve()} on object before returning.
	 * @return The property with the specified name, or <jk>null</jk> if no property is specified.
	 */
	public JsonSchema<?> getProperty(String name, boolean resolve) {
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
	 * Bean property getter:  <property>readOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @return The value of the <property>readOnly</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getReadOnly() { return readOnly; }

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The value of the <property>$ref</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("$ref")
	public URI getRef() { return ref; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * @return The value of the <property>required</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<String> getRequired() { return u(required); }

	/**
	 * Bean property getter:  <property>$schema</property>.
	 *
	 * @return The value of the <property>$schema</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("$schema")
	public URI getSchemaVersionUri() { return schemaVersion; }

	/**
	 * Bean property getter:  <property>then</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @return The value of the <property>then</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	@BeanProp("then")
	public JsonSchema<?> getThen() { return then_; }

	/**
	 * Bean property getter:  <property>summary</property>.
	 *
	 * <p>
	 * A short, concise summary of the schema's purpose, intended for AI/LLM consumption, compact
	 * documentation, or any context where brevity is important. Unlike {@link #getDescription()},
	 * which can be multi-line and detailed, this value should be a single sentence or phrase.
	 *
	 * <p>
	 * This is a Juneau extension. It serializes as the JSON Schema keyword <c>"summary"</c>.
	 *
	 * @return The value of the <property>summary</property> property, or <jk>null</jk> if it is not set.
	 * @since 10.0.0
	 */
	public String getSummary() { return summary; }

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * @return The value of the <property>title</property> property, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() { return title; }

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
	 * Bean property getter:  <property>unevaluatedItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @return The value of the <property>unevaluatedItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema<?> getUnevaluatedItems() { return unevaluatedItems; }

	/**
	 * Bean property getter:  <property>unevaluatedProperties</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @return The value of the <property>unevaluatedProperties</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public JsonSchema<?> getUnevaluatedProperties() { return unevaluatedProperties; }

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return
	 * 	The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() { return uniqueItems; }

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
	 * @return The referenced schema, or <jk>null</jk> if this schema is a <property>$ref</property> whose target is not found in the registered schema map.
	 */
	public JsonSchema<?> resolve() {
		if (ref == null || master.schemaMap == null)
			return this;
		return master.schemaMap.get(ref);
	}

	/**
	 * Bean property setter:  <property>additionalItems</property>.
	 *
	 * @param value
	 * 	The new value for the <property>additionalItems</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link JsonSchemaArray}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public SELF setAdditionalItems(Object value) {
		this.additionalItemsBoolean = null;
		this.additionalItemsSchemaArray = null;
		if (nn(value)) {
			if (value instanceof Boolean value2)
				this.additionalItemsBoolean = value2;
			else if (value instanceof JsonSchemaArray value2) {
				this.additionalItemsSchemaArray = value2;
				setMasterOn(this.additionalItemsSchemaArray);
			} else {
				throw brex(JsonSchemaProperty.class, "Invalid attribute type '%s' passed in.  Must be one of the following:  Boolean, JsonSchemaArray", cn(value));
			}
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>additionalProperties</property>.
	 *
	 * @param value
	 * 	The new value for the <property>additionalProperties</property> property on this bean.
	 * 	This object must be of type {@link Boolean} or {@link JsonSchema}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	@MarshalledProp(dictionary={ JsonSchema.class })
	public SELF setAdditionalProperties(Object value) {
		this.additionalPropertiesBoolean = null;
		this.additionalPropertiesSchema = null;
		if (nn(value)) {
			if (value instanceof Boolean value2)
				this.additionalPropertiesBoolean = value2;
			else if (value instanceof JsonSchema<?> value2) {
				this.additionalPropertiesSchema = value2;
				setMasterOn(this.additionalPropertiesSchema);
			} else
				throw brex(JsonSchemaProperty.class, "Invalid attribute type '%s' passed in.  Must be one of the following:  Boolean, JsonSchema", cn(value));
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value The new value for the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setAllOf(List<JsonSchema<?>> value) {
		this.allOf = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value The new value for the <property>allOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setAllOf(JsonSchema<?>...value) {
		return setAllOf(list(value));
	}

	/**
	 * Bean property setter:  <property>anyOf</property>.
	 *
	 * @param value The new value of the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setAnyOf(List<JsonSchema<?>> value) {
		this.anyOf = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>anyOf</property>.
	 *
	 * @param value The new value for the <property>anyOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setAnyOf(JsonSchema<?>...value) {
		return setAnyOf(list(value));
	}

	/**
	 * Bean property setter:  <property>const</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @param value The new value for the <property>const</property> property on this bean.
	 * @return This object.
	 */
	public SELF setConst(Object value) {
		this.const_ = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>contentEncoding</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param value The new value for the <property>contentEncoding</property> property on this bean.
	 * @return This object.
	 */
	public SELF setContentEncoding(String value) {
		this.contentEncoding = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>contentMediaType</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param value The new value for the <property>contentMediaType</property> property on this bean.
	 * @return This object.
	 */
	public SELF setContentMediaType(String value) {
		this.contentMediaType = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @param value The new value for the <property>deprecated</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDeprecated(Boolean value) {
		this.deprecated = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>$comment</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param value The new value for the <property>$comment</property> property on this bean.
	 * @return This object.
	 */
	@BeanProp("$comment")
	public SELF setComment(String value) {
		this.comment = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>definitions</property>.
	 *
	 * @param value The new value for the <property>definitions</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDefinitions(Map<String,JsonSchema<?>> value) {
		this.definitions = value;
		if (nn(value))
			setMasterOn(value.values());
		return self();
	}

	/**
	 * Bean property setter:  <property>$defs</property>.
	 *
	 * <p>
	 * This is the Draft 2020-12 replacement for <property>definitions</property>.
	 * Both properties are supported for backward compatibility.
	 *
	 * @param value The new value for the <property>$defs</property> property on this bean.
	 * @return This object.
	 */
	@BeanProp("$defs")
	public SELF setDefs(Map<String,JsonSchema<?>> value) {
		this.defs = value;
		if (nn(value))
			setMasterOn(value.values());
		return self();
	}

	/**
	 * Bean property setter:  <property>dependencies</property>.
	 *
	 * @param value The new value for the <property>dependencies</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDependencies(Map<String,JsonSchema<?>> value) {
		this.dependencies = value;
		if (nn(value))
			setMasterOn(value.values());
		return self();
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 *
	 * @param value The new value for the <property>format</property> property on this bean.
	 * @return This object.
	 */
	public SELF setFormat(String value) {
		this.format = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>dependentRequired</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the array form of <property>dependencies</property>.
	 *
	 * @param value The new value for the <property>dependentRequired</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDependentRequired(Map<String,List<String>> value) {
		this.dependentRequired = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>dependentSchemas</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09 as a replacement for the schema form of <property>dependencies</property>.
	 *
	 * @param value The new value for the <property>dependentSchemas</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDependentSchemas(Map<String,JsonSchema<?>> value) {
		this.dependentSchemas = value;
		if (nn(value))
			setMasterOn(value.values());
		return self();
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * @param value The new value for the <property>description</property> property on this bean.
	 * @return This object.
	 */
	public SELF setDescription(String value) {
		this.description = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>else</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param value The new value for the <property>else</property> property on this bean.
	 * @return This object.
	 */
	@BeanProp("else")
	public SELF setElse(JsonSchema<?> value) {
		this.else_ = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value The new value for the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setEnum(List<Object> value) {
		this.enum_ = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value The new value for the <property>enum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setEnum(Object...value) {
		return setEnum(list(value));
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @param value The new value for the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public SELF setExamples(List<Object> value) {
		this.examples = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * This property was added in Draft 06.
	 *
	 * @param value The new value for the <property>examples</property> property on this bean.
	 * @return This object.
	 */
	public SELF setExamples(Object...value) {
		return setExamples(list(value));
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive upper bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @param value The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setExclusiveMaximum(Number value) {
		this.exclusiveMaximum = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * <p>
	 * In Draft 06+, this is a numeric value representing the exclusive lower bound.
	 * In Draft 04, this was a boolean flag. This implementation uses the Draft 06+ semantics.
	 *
	 * @param value The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setExclusiveMinimum(Number value) {
		this.exclusiveMinimum = value;
		return self();
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
	 * @param value The new value for the <property>id</property> property on this bean.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 * @deprecated Use {@link #setIdUri(Object)} instead.
	 */
	@Deprecated(since = "10.0", forRemoval = true)
	@SuppressWarnings({
		"java:S1133" // Kept for Draft 04 backward compatibility, will be removed in future version
	})
	public SELF setId(Object value) {
		this.id = toUri(value);
		return self();
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
	 * @param value The new value for the <property>$id</property> property on this bean.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@BeanProp("$id")
	public SELF setIdUri(Object value) {
		this.idUri = toUri(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>if</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param value The new value for the <property>if</property> property on this bean.
	 * @return This object.
	 */
	@BeanProp("if")
	public SELF setIf(JsonSchema<?> value) {
		this.if_ = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 *
	 * @param value
	 * 	The new value for the <property>items</property> property on this bean.
	 * 	This object must be of type {@link JsonSchema} or {@link JsonSchemaArray}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public SELF setItems(Object value) {
		this.itemsSchema = null;
		this.itemsSchemaArray = null;
		if (nn(value)) {
			if (value instanceof JsonSchema<?> value2) {
				this.itemsSchema = value2;
				setMasterOn(this.itemsSchema);
			} else if (value instanceof JsonSchemaArray value2) {
				this.itemsSchemaArray = value2;
				setMasterOn(this.itemsSchemaArray);
			} else {
				throw brex(JsonSchemaProperty.class, "Invalid attribute type '%s' passed in.  Must be one of the following:  JsonSchema, JsonSchemaArray", cn(value));
			}
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * @param value The new value for the <property>maximum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMaximum(Number value) {
		this.maximum = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * @param value The new value for the <property>maxItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMaxItems(Integer value) {
		this.maxItems = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * @param value The new value for the <property>maxLength</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMaxLength(Integer value) {
		this.maxLength = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>maxProperties</property>.
	 *
	 * @param value The new value for the <property>maxProperties</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMaxProperties(Integer value) {
		this.maxProperties = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * @param value The new value for the <property>minimum</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMinimum(Number value) {
		this.minimum = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * @param value The new value for the <property>minItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMinItems(Integer value) {
		this.minItems = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * @param value The new value for the <property>minLength</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMinLength(Integer value) {
		this.minLength = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>minProperties</property>.
	 *
	 * @param value The new value for the <property>minProperties</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMinProperties(Integer value) {
		this.minProperties = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * @param value The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setMultipleOf(Number value) {
		this.multipleOf = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 *
	 * <p>
	 * This is an internal property used for tracking property names and is not part of the JSON Schema specification.
	 *
	 * @param value The new value for the <property>name</property> property on this bean.
	 * @return This object.
	 */
	@BeanIgnore
	public SELF setName(String value) {
		this.name = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>not</property>.
	 *
	 * @param value The new value for the <property>not</property> property on this bean.
	 * @return This object.
	 */
	public SELF setNot(JsonSchema<?> value) {
		this.not = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>oneOf</property>.
	 *
	 * @param value The new value for the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setOneOf(List<JsonSchema<?>> value) {
		this.oneOf = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>oneOf</property>.
	 *
	 * @param value The new value for the <property>oneOf</property> property on this bean.
	 * @return This object.
	 */
	public SELF setOneOf(JsonSchema<?>...value) {
		return setOneOf(list(value));
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 *
	 * @param value The new value for the <property>pattern</property> property on this bean.
	 * @return This object.
	 */
	public SELF setPattern(String value) {
		this.pattern = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>patternProperties</property>.
	 *
	 * @param value The new value for the <property>patternProperties</property> property on this bean.
	 * @return This object.
	 */
	public SELF setPatternProperties(Map<String,JsonSchema<?>> value) {
		this.patternProperties = value;
		if (nn(value)) {
			value.entrySet().forEach(x -> {
				var s = x.getValue();
				setMasterOn(s);
				s.setName(x.getKey());
			});
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>prefixItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2020-12 for tuple validation.
	 *
	 * @param value The new value for the <property>prefixItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF setPrefixItems(JsonSchemaArray value) {
		this.prefixItems = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param value The new value for the <property>properties</property> property on this bean.
	 * @return This object.
	 */
	public SELF setProperties(Map<String,JsonSchema<?>> value) {
		this.properties = value;
		if (nn(value)) {
			value.entrySet().forEach(x -> {
				var v = x.getValue();
				setMasterOn(v);
				v.setName(x.getKey());
			});
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>readOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param value The new value for the <property>readOnly</property> property on this bean.
	 * @return This object.
	 */
	public SELF setReadOnly(Boolean value) {
		this.readOnly = value;
		return self();
	}

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
	 * @param value The new value for the <property>$ref</property> property on this bean.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@BeanProp("$ref")
	public SELF setRef(Object value) {
		this.ref = toUri(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 *
	 * @param value The new value for the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public SELF setRequired(List<String> value) {
		this.required = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 *
	 * @param value The new value for the <property>required</property> property on this bean.
	 * @return This object.
	 */
	public SELF setRequired(String...value) {
		return setRequired(list(value));
	}

	/**
	 * Associates a schema map with this schema for resolving other schemas identified through <property>$ref</property>
	 * properties.
	 *
	 * @param value The schema map to associate with this schema.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@BeanIgnore
	public SELF setSchemaMap(JsonSchemaMap value) {
		this.schemaMap = value;
		return self();
	}

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
	 * @param value The new value for the <property>schemaVersion</property> property on this bean.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@BeanProp("$schema")
	public SELF setSchemaVersionUri(Object value) {
		this.schemaVersion = toUri(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>then</property>.
	 *
	 * <p>
	 * This property was added in Draft 07 for conditional schema application.
	 *
	 * @param value The new value for the <property>then</property> property on this bean.
	 * @return This object.
	 */
	@BeanProp("then")
	public SELF setThen(JsonSchema<?> value) {
		this.then_ = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>summary</property>.
	 *
	 * @param value The new value for the <property>summary</property> property on this bean.
	 * @return This object.
	 * @since 10.0.0
	 */
	public SELF setSummary(String value) {
		this.summary = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 *
	 * @param value The new value for the <property>title</property> property on this bean.
	 * @return This object.
	 */
	public SELF setTitle(String value) {
		this.title = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * @param value
	 * 	The new value for the <property>type</property> property on this bean.
	 * 	This object must be of type {@link JsonType} or {@link JsonTypeArray}.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 * @throws BeanRuntimeException If invalid object type passed in.
	 */
	public SELF setType(Object value) {
		this.typeJsonType = null;
		this.typeJsonTypeArray = null;
		if (nn(value)) {
			if (value instanceof JsonType value2)
				this.typeJsonType = value2;
			else if (value instanceof JsonTypeArray value2)
				this.typeJsonTypeArray = value2;
			else
				throw brex(JsonSchemaProperty.class, "Invalid attribute type '%s' passed in.  Must be one of the following:  SimpleType, SimpleTypeArray", cn(value));
		}
		return self();
	}

	/**
	 * Bean property setter:  <property>unevaluatedItems</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @param value The new value for the <property>unevaluatedItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF setUnevaluatedItems(JsonSchema<?> value) {
		this.unevaluatedItems = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>unevaluatedProperties</property>.
	 *
	 * <p>
	 * This property was added in Draft 2019-09.
	 *
	 * @param value The new value for the <property>unevaluatedProperties</property> property on this bean.
	 * @return This object.
	 */
	public SELF setUnevaluatedProperties(JsonSchema<?> value) {
		this.unevaluatedProperties = value;
		setMasterOn(value);
		return self();
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * @param value The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object.
	 */
	public SELF setUniqueItems(Boolean value) {
		this.uniqueItems = value;
		return self();
	}

	/**
	 * Bean property setter:  <property>writeOnly</property>.
	 *
	 * <p>
	 * This property was added in Draft 07.
	 *
	 * @param value The new value for the <property>writeOnly</property> property on this bean.
	 * @return This object.
	 */
	public SELF setWriteOnly(Boolean value) {
		this.writeOnly = value;
		return self();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json.of(this);
	}

	private void setMasterOn(Collection<JsonSchema<?>> ss) {
		if (nn(ss))
			ss.forEach(this::setMasterOn);
	}

	private void setMasterOn(JsonSchema<?> s) {
		if (nn(s))
			s.setMaster(this);
	}

	private void setMasterOn(JsonSchema<?>[] ss) {
		assertArgNotNull("ss", ss);
		for (var s : ss)
			setMasterOn(s);
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
	 * @param master The master schema to associate on this and all children.  Can be <jk>null</jk> to clear the master reference on this schema and all children.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for this logic
	})
	protected void setMaster(JsonSchema<?> master) {
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
		if (nn(if_))
			if_.setMaster(master);
		if (nn(then_))
			then_.setMaster(master);
		if (nn(else_))
			else_.setMaster(master);
	}
}
