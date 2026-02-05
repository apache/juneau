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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;

/**
 * Allows the definition of input and output data types.
 *
 * <p>
 * The Schema Object allows the definition of input and output data types for Swagger 2.0, including objects,
 * primitives, and arrays. This object is an extended subset of the JSON Schema Specification Draft 4, with
 * additional extensions provided by the Swagger Specification to allow for more complete documentation.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Schema Object supports all properties from JSON Schema Draft 4, including but not limited to:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string) - The data type. Values: <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js>, <js>"object"</js>, <js>"file"</js>
 * 	<li><c>format</c> (string) - The format modifier (e.g., <js>"int32"</js>, <js>"int64"</js>, <js>"float"</js>, <js>"double"</js>, <js>"date"</js>, <js>"date-time"</js>)
 * 	<li><c>title</c> (string) - A short title for the schema
 * 	<li><c>description</c> (string) - A description of the schema
 * 	<li><c>default</c> (any) - The default value
 * 	<li><c>multipleOf</c> (number) - Must be a multiple of this value
 * 	<li><c>maximum</c> (number) - Maximum value (inclusive by default)
 * 	<li><c>exclusiveMaximum</c> (boolean) - If true, maximum is exclusive
 * 	<li><c>minimum</c> (number) - Minimum value (inclusive by default)
 * 	<li><c>exclusiveMinimum</c> (boolean) - If true, minimum is exclusive
 * 	<li><c>maxLength</c> (integer) - Maximum string length
 * 	<li><c>minLength</c> (integer) - Minimum string length
 * 	<li><c>pattern</c> (string) - Regular expression pattern the string must match
 * 	<li><c>maxItems</c> (integer) - Maximum array length
 * 	<li><c>minItems</c> (integer) - Minimum array length
 * 	<li><c>uniqueItems</c> (boolean) - If true, array items must be unique
 * 	<li><c>maxProperties</c> (integer) - Maximum number of object properties
 * 	<li><c>minProperties</c> (integer) - Minimum number of object properties
 * 	<li><c>required</c> (array of string) - Required property names
 * 	<li><c>enum</c> (array) - Possible values for this schema
 * 	<li><c>properties</c> (map of {@link SchemaInfo}) - Object property definitions
 * 	<li><c>items</c> ({@link Items}) - Schema for array items
 * 	<li><c>allOf</c> (array of {@link SchemaInfo}) - Must validate against all schemas
 * 	<li><c>discriminator</c> (string) - Property name for polymorphism (Swagger extension)
 * 	<li><c>readOnly</c> (boolean) - Relevant only for Schema properties (Swagger extension)
 * 	<li><c>xml</c> ({@link Xml}) - XML representation details (Swagger extension)
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation (Swagger extension)
 * 	<li><c>example</c> (any) - Example value (Swagger extension)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	SchemaInfo <jv>info</jv> = <jsm>schemaInfo</jsm>()
 * 		.type(<js>"string"</js>)
 * 		.title(<js>"foo"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>info</jv>);
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
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#schema-object">Swagger 2.0 Specification &gt; Schema Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/data-models/">Swagger Data Models</a>
 * 	<li class='link'><a class="doclink" href="https://json-schema.org/">JSON Schema Specification</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
@SuppressWarnings({"java:S115", "java:S116"})
public class SchemaInfo extends SwaggerElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_key = "key";
	private static final String ARG_property = "property";
	private static final String ARG_value = "value";

	// Property name constants
	private static final String PROP_additionalProperties = "additionalProperties";
	private static final String PROP_allOf = "allOf";
	private static final String PROP_default = "default";
	private static final String PROP_description = "description";
	private static final String PROP_discriminator = "discriminator";
	private static final String PROP_enum = "enum";
	private static final String PROP_example = "example";
	private static final String PROP_exclusiveMaximum = "exclusiveMaximum";
	private static final String PROP_exclusiveMinimum = "exclusiveMinimum";
	private static final String PROP_externalDocs = "externalDocs";
	private static final String PROP_format = "format";
	private static final String PROP_items = "items";
	private static final String PROP_maxItems = "maxItems";
	private static final String PROP_maxLength = "maxLength";
	private static final String PROP_maxProperties = "maxProperties";
	private static final String PROP_maximum = "maximum";
	private static final String PROP_minItems = "minItems";
	private static final String PROP_minLength = "minLength";
	private static final String PROP_minProperties = "minProperties";
	private static final String PROP_minimum = "minimum";
	private static final String PROP_multipleOf = "multipleOf";
	private static final String PROP_pattern = "pattern";
	private static final String PROP_properties = "properties";
	private static final String PROP_readOnly = "readOnly";
	private static final String PROP_required = "required";
	private static final String PROP_requiredProperties = "requiredProperties";
	private static final String PROP_title = "title";
	private static final String PROP_type = "type";
	private static final String PROP_uniqueItems = "uniqueItems";
	private static final String PROP_xml = "xml";
	private static final String PROP_ref = "$ref";

	private String format;
	private String title;
	private String description;
	private String pattern;
	private String type;
	private String discriminator;
	private String ref;
	private Number multipleOf;
	private Number maximum;
	private Number minimum;
	private Integer maxLength;
	private Integer minLength;
	private Integer maxItems;
	private Integer minItems;
	private Integer maxProperties;
	private Integer minProperties;
	private Boolean exclusiveMaximum;
	private Boolean exclusiveMinimum;
	private Boolean uniqueItems;
	private Boolean readOnly;
	private Boolean required;
	private Object default_;
	private Object example;
	private Items items;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private Set<Object> enum_ = new LinkedHashSet<>();
	private Set<SchemaInfo> allOf = new LinkedHashSet<>();
	private Set<String> requiredProperties = new LinkedHashSet<>();
	private Map<String,SchemaInfo> properties = map();
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
		if (nn(copyFrom.allOf))
			this.allOf.addAll(copyOf(copyFrom.allOf, SchemaInfo::copy));
		this.default_ = copyFrom.default_;
		this.description = copyFrom.description;
		this.discriminator = copyFrom.discriminator;
		if (nn(copyFrom.enum_))
			this.enum_.addAll(copyOf(copyFrom.enum_));
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
		if (nn(copyFrom.requiredProperties))
			this.requiredProperties.addAll(copyOf(copyFrom.requiredProperties));
		this.title = copyFrom.title;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
		this.xml = copyFrom.xml == null ? null : copyFrom.xml.copy();
		if (nn(copyFrom.properties))
			properties.putAll(copyOf(copyFrom.properties, SchemaInfo::copy));
	}

	/**
	 * Bean property fluent setter:  <property>allOf</property>.
	 *
	 * <p>
	 * Inline or referenced schema MUST be of a Schema Object and not a standard JSON Schema.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addAllOf(Collection<SchemaInfo> values) {
		if (nn(values))
			for (var v : values)
				if (nn(v))
					allOf.add(v);
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
	public SchemaInfo addAllOf(SchemaInfo...values) {
		if (nn(values))
			for (var v : values)
				if (nn(v))
					allOf.add(v);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * <p>
	 * An enumeration of possible values.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addEnum(Collection<Object> values) {
		if (nn(values))
			enum_.addAll(values);
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
		if (nn(value))
			for (var v : value)
				if (nn(v))
					enum_.add(v);
		return this;
	}

	/**
	 * Bean property appender:  <property>properties</property>.
	 *
	 * @param key The property key.  Must not be <jk>null</jk>.
	 * @param value The property value.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addProperty(String key, SchemaInfo value) {
		assertArgNotNull(ARG_key, key);
		assertArgNotNull(ARG_value, value);
		properties.put(key, value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>requiredProperties</property>.
	 *
	 * <p>
	 * Takes an array of strings that define the required properties.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public SchemaInfo addRequiredProperties(Collection<String> values) {
		if (nn(values))
			requiredProperties.addAll(values);
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
		if (nn(value))
			for (var v : value)
				if (nn(v))
					requiredProperties.add(v);
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public SchemaInfo copy() {
		return new SchemaInfo(this);
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_additionalProperties -> toType(getAdditionalProperties(), type);
			case PROP_allOf -> toType(getAllOf(), type);
			case PROP_default -> toType(getDefault(), type);
			case PROP_description -> toType(getDescription(), type);
			case PROP_discriminator -> toType(getDiscriminator(), type);
			case PROP_enum -> toType(getEnum(), type);
			case PROP_example -> toType(getExample(), type);
			case PROP_exclusiveMaximum -> toType(getExclusiveMaximum(), type);
			case PROP_exclusiveMinimum -> toType(getExclusiveMinimum(), type);
			case PROP_externalDocs -> toType(getExternalDocs(), type);
			case PROP_format -> toType(getFormat(), type);
			case PROP_items -> toType(getItems(), type);
			case PROP_maximum -> toType(getMaximum(), type);
			case PROP_maxItems -> toType(getMaxItems(), type);
			case PROP_maxLength -> toType(getMaxLength(), type);
			case PROP_maxProperties -> toType(getMaxProperties(), type);
			case PROP_minimum -> toType(getMinimum(), type);
			case PROP_minItems -> toType(getMinItems(), type);
			case PROP_minLength -> toType(getMinLength(), type);
			case PROP_minProperties -> toType(getMinProperties(), type);
			case PROP_multipleOf -> toType(getMultipleOf(), type);
			case PROP_pattern -> toType(getPattern(), type);
			case PROP_properties -> toType(getProperties(), type);
			case PROP_readOnly -> toType(getReadOnly(), type);
			case PROP_ref -> toType(getRef(), type);
			case PROP_required -> toType(getRequired(), type);
			case PROP_requiredProperties -> toType(getRequiredProperties(), type);
			case PROP_title -> toType(getTitle(), type);
			case PROP_type -> toType(getType(), type);
			case PROP_uniqueItems -> toType(getUniqueItems(), type);
			case PROP_xml -> toType(getXml(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>additionalProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getAdditionalProperties() { return additionalProperties; }

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<SchemaInfo> getAllOf() { return nullIfEmpty(allOf); }

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Unlike JSON Schema, the value MUST conform to the defined type for the Schema Object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() { return default_; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>discriminator</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDiscriminator() { return discriminator; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getEnum() { return nullIfEmpty(enum_); }

	/**
	 * Bean property getter:  <property>example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getExample() { return example; }

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() { return exclusiveMaximum; }

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() { return exclusiveMinimum; }

	/**
	 * Bean property getter:  <property>externalDocs</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public ExternalDocumentation getExternalDocs() { return externalDocs; }

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() { return format; }

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() { return items; }

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() { return maximum; }

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() { return maxItems; }

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() { return maxLength; }

	/**
	 * Bean property getter:  <property>maxProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxProperties() { return maxProperties; }

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() { return minimum; }

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() { return minItems; }

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() { return minLength; }

	/**
	 * Bean property getter:  <property>minProperties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinProperties() { return minProperties; }

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() { return multipleOf; }

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() { return pattern; }

	/**
	 * Bean property getter:  <property>properties</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,SchemaInfo> getProperties() { return nullIfEmpty(properties); }

	/**
	 * Bean property getter:  <property>readOnly</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getReadOnly() { return readOnly; }

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() { return ref; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() { return required; }

	/**
	 * Bean property getter:  <property>requiredProperties</property>.
	 *
	 * <p>
	 * The list of required properties.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<String> getRequiredProperties() { return nullIfEmpty(requiredProperties); }

	/**
	 * Bean property getter:  <property>title</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() { return title; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() { return uniqueItems; }

	/**
	 * Bean property getter:  <property>xml</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Xml getXml() { return xml; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(ref), PROP_ref)
			.addIf(nn(additionalProperties), PROP_additionalProperties)
			.addIf(ne(allOf), PROP_allOf)
			.addIf(nn(default_), PROP_default)
			.addIf(nn(description), PROP_description)
			.addIf(nn(discriminator), PROP_discriminator)
			.addIf(ne(enum_), PROP_enum)
			.addIf(nn(example), PROP_example)
			.addIf(nn(exclusiveMaximum), PROP_exclusiveMaximum)
			.addIf(nn(exclusiveMinimum), PROP_exclusiveMinimum)
			.addIf(nn(externalDocs), PROP_externalDocs)
			.addIf(nn(format), PROP_format)
			.addIf(nn(items), PROP_items)
			.addIf(nn(maxItems), PROP_maxItems)
			.addIf(nn(maxLength), PROP_maxLength)
			.addIf(nn(maxProperties), PROP_maxProperties)
			.addIf(nn(maximum), PROP_maximum)
			.addIf(nn(minItems), PROP_minItems)
			.addIf(nn(minLength), PROP_minLength)
			.addIf(nn(minProperties), PROP_minProperties)
			.addIf(nn(minimum), PROP_minimum)
			.addIf(nn(multipleOf), PROP_multipleOf)
			.addIf(nn(pattern), PROP_pattern)
			.addIf(ne(properties), PROP_properties)
			.addIf(nn(readOnly), PROP_readOnly)
			.addIf(nn(required), PROP_required)
			.addIf(ne(requiredProperties), PROP_requiredProperties)
			.addIf(nn(title), PROP_title)
			.addIf(nn(type), PROP_type)
			.addIf(nn(uniqueItems), PROP_uniqueItems)
			.addIf(nn(xml), PROP_xml)
			.build();
		// @formatter:on
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

		if (nn(ref)) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = swagger.findRef(ref, SchemaInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (nn(items))
			items = items.resolveRefs(swagger, refStack, maxDepth);

		properties.entrySet().forEach(x -> x.setValue(x.getValue().resolveRefs(swagger, refStack, maxDepth)));

		if (nn(additionalProperties))
			additionalProperties = additionalProperties.resolveRefs(swagger, refStack, maxDepth);

		this.example = null;

		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public SchemaInfo set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_additionalProperties -> setAdditionalProperties(toType(value, SchemaInfo.class));
			case PROP_allOf -> setAllOf(toSetBuilder(value, SchemaInfo.class).sparse().build());
			case PROP_default -> setDefault(value);
			case PROP_description -> setDescription(s(value));
			case PROP_discriminator -> setDiscriminator(s(value));
			case PROP_enum -> setEnum(value);
			case PROP_example -> setExample(value);
			case PROP_exclusiveMaximum -> setExclusiveMaximum(toBoolean(value));
			case PROP_exclusiveMinimum -> setExclusiveMinimum(toBoolean(value));
			case PROP_externalDocs -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case PROP_format -> setFormat(s(value));
			case PROP_items -> setItems(toType(value, Items.class));
			case PROP_maximum -> setMaximum(toNumber(value));
			case PROP_maxItems -> setMaxItems(toInteger(value));
			case PROP_maxLength -> setMaxLength(toInteger(value));
			case PROP_maxProperties -> setMaxProperties(toInteger(value));
			case PROP_minimum -> setMinimum(toNumber(value));
			case PROP_minItems -> setMinItems(toInteger(value));
			case PROP_minLength -> setMinLength(toInteger(value));
			case PROP_minProperties -> setMinProperties(toInteger(value));
			case PROP_multipleOf -> setMultipleOf(toNumber(value));
			case PROP_pattern -> setPattern(s(value));
			case PROP_properties -> setProperties(toMapBuilder(value, String.class, SchemaInfo.class).sparse().build());
			case PROP_readOnly -> setReadOnly(toBoolean(value));
			case PROP_ref -> setRef(s(value));
			case PROP_required -> setRequired(toBoolean(value));
			case PROP_requiredProperties -> setRequiredProperties(listb(String.class).addAny(value).sparse().build());
			case PROP_title -> setTitle(s(value));
			case PROP_type -> setType(s(value));
			case PROP_uniqueItems -> setUniqueItems(toBoolean(value));
			case PROP_xml -> setXml(toType(value, Xml.class));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setAllOf(Collection<SchemaInfo> value) {
		allOf.clear();
		if (nn(value))
			allOf.addAll(value);
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
	public SchemaInfo setAllOf(SchemaInfo...value) {
		setAllOf(setb(SchemaInfo.class).sparse().addAny((Object[])value).build());
		return this;
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
		default_ = value;
		return this;
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
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setEnum(Collection<Object> value) {
		enum_.clear();
		if (nn(value))
			enum_.addAll(value);
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
		setEnum(setb(Object.class).sparse().addAny(value).build());
		return this;
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
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public SchemaInfo setProperties(Map<String,SchemaInfo> value) {
		properties.clear();
		if (nn(value))
			properties.putAll(value);
		return this;
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
		requiredProperties.clear();
		if (nn(value))
			requiredProperties.addAll(value);
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
		setRequiredProperties(setb(String.class).sparse().addAny((Object[])value).build());
		return this;
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

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public SchemaInfo strict() {
		super.strict();
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	@Override
	public SchemaInfo strict(Object value) {
		super.strict(value);
		return this;
	}
}