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
package org.apache.juneau.bean.openapi3;

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
 * The Schema Object allows the definition of input and output data types, including objects, primitives, and arrays.
 * This object is an extended subset of the JSON Schema Specification Draft 4, with additional extensions provided
 * by the OpenAPI Specification to allow for more complete documentation.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Schema Object supports all properties from JSON Schema Draft 4, including but not limited to:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string) - The data type. Values: <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js>, <js>"object"</js>
 * 	<li><c>format</c> (string) - The format modifier (e.g., <js>"int32"</js>, <js>"int64"</js>, <js>"float"</js>, <js>"double"</js>, <js>"date"</js>, <js>"date-time"</js>)
 * 	<li><c>title</c> (string) - A short title for the schema
 * 	<li><c>description</c> (string) - A description of the schema (CommonMark syntax may be used)
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
 * 	<li><c>oneOf</c> (array of {@link SchemaInfo}) - Must validate against exactly one schema
 * 	<li><c>anyOf</c> (array of {@link SchemaInfo}) - Must validate against any schema
 * 	<li><c>not</c> ({@link SchemaInfo}) - Must not validate against this schema
 * 	<li><c>nullable</c> (boolean) - Allows the value to be null (OpenAPI 3.0 extension)
 * 	<li><c>discriminator</c> ({@link Discriminator}) - Discriminator for polymorphism (OpenAPI extension)
 * 	<li><c>readOnly</c> (boolean) - Relevant only for Schema properties (OpenAPI extension)
 * 	<li><c>writeOnly</c> (boolean) - Relevant only for Schema properties (OpenAPI extension)
 * 	<li><c>xml</c> ({@link Xml}) - XML representation details (OpenAPI extension)
 * 	<li><c>externalDocs</c> ({@link ExternalDocumentation}) - Additional external documentation (OpenAPI extension)
 * 	<li><c>example</c> (any) - Example value (OpenAPI extension)
 * 	<li><c>deprecated</c> (boolean) - Specifies that the schema is deprecated (OpenAPI extension)
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a schema for a Pet object</jc>
 * 	SchemaInfo <jv>schema</jv> = <jk>new</jk> SchemaInfo()
 * 		.setType(<js>"object"</js>)
 * 		.setRequired(<js>"id"</js>, <js>"name"</js>)
 * 		.setProperties(
 * 			JsonMap.<jsm>of</jsm>(
 * 				<js>"id"</js>, <jk>new</jk> SchemaInfo().setType(<js>"integer"</js>).setFormat(<js>"int64"</js>),
 * 				<js>"name"</js>, <jk>new</jk> SchemaInfo().setType(<js>"string"</js>),
 * 				<js>"tag"</js>, <jk>new</jk> SchemaInfo().setType(<js>"string"</js>)
 * 			)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#schema-object">OpenAPI Specification &gt; Schema Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/data-models/">OpenAPI Data Models</a>
 * 	<li class='link'><a class="doclink" href="https://json-schema.org/">JSON Schema Specification</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@SuppressWarnings({"java:S115", "java:S116"})
public class SchemaInfo extends OpenApiElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_property = "property";

	// Property name constants
	private static final String PROP_additionalProperties = "additionalProperties";
	private static final String PROP_allOf = "allOf";
	private static final String PROP_anyOf = "anyOf";
	private static final String PROP_default = "default";
	private static final String PROP_deprecated = "deprecated";
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
	private static final String PROP_not = "not";
	private static final String PROP_nullable = "nullable";
	private static final String PROP_oneOf = "oneOf";
	private static final String PROP_pattern = "pattern";
	private static final String PROP_properties = "properties";
	private static final String PROP_readOnly = "readOnly";
	private static final String PROP_required = "required";
	private static final String PROP_title = "title";
	private static final String PROP_type = "type";
	private static final String PROP_uniqueItems = "uniqueItems";
	private static final String PROP_writeOnly = "writeOnly";
	private static final String PROP_xml = "xml";
	private static final String PROP_ref = "$ref";

	private String format;
	private String title;
	private String description;
	private String pattern;
	private String ref;
	private String type;
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
	private Boolean nullable;
	private Boolean writeOnly;
	private Boolean readOnly;
	private Boolean deprecated;
	private Object default_;
	private Object example;
	private Items items;
	private Xml xml;
	private ExternalDocumentation externalDocs;
	private List<Object> allOf = list();
	private List<Object> oneOf = list();
	private List<Object> anyOf = list();
	private List<Object> enum_ = list();
	private List<String> required = list();
	private Discriminator discriminator;
	private Map<String,SchemaInfo> properties;
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
		this.default_ = copyFrom.default_;
		this.example = copyFrom.example;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.xml = copyFrom.xml == null ? null : copyFrom.xml.copy();
		this.externalDocs = copyFrom.externalDocs == null ? null : copyFrom.externalDocs.copy();
		if (nn(copyFrom.enum_))
			enum_.addAll(copyFrom.enum_);
		if (nn(copyFrom.allOf))
			allOf.addAll(copyFrom.allOf);
		if (nn(copyFrom.required))
			required.addAll(copyFrom.required);
		if (nn(copyFrom.anyOf))
			anyOf.addAll(copyFrom.anyOf);
		if (nn(copyFrom.oneOf))
			oneOf.addAll(copyFrom.oneOf);
		this.properties = copyOf(copyFrom.properties, SchemaInfo::copy);
		this.additionalProperties = copyFrom.additionalProperties == null ? null : copyFrom.additionalProperties.copy();
		this.not = copyFrom.not == null ? null : copyFrom.not.copy();
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					allOf.add(v);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					anyOf.add(v);
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
	 * 	enum_(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	enum_(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public SchemaInfo addEnum(Object...values) {
		if (nn(values))
			for (var v : values)
				if (nn(v))
					enum_.add(v);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					oneOf.add(v);
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
		if (nn(values))
			for (var v : values)
				if (nn(v))
					required.add(v);
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
			case PROP_format -> toType(getFormat(), type);
			case PROP_title -> toType(getTitle(), type);
			case PROP_description -> toType(getDescription(), type);
			case PROP_default -> toType(getDefault(), type);
			case PROP_multipleOf -> toType(getMultipleOf(), type);
			case PROP_maximum -> toType(getMaximum(), type);
			case PROP_exclusiveMaximum -> toType(getExclusiveMaximum(), type);
			case PROP_minimum -> toType(getMinimum(), type);
			case PROP_exclusiveMinimum -> toType(getExclusiveMinimum(), type);
			case PROP_maxLength -> toType(getMaxLength(), type);
			case PROP_minLength -> toType(getMinLength(), type);
			case PROP_pattern -> toType(getPattern(), type);
			case PROP_maxItems -> toType(getMaxItems(), type);
			case PROP_minItems -> toType(getMinItems(), type);
			case PROP_uniqueItems -> toType(getUniqueItems(), type);
			case PROP_maxProperties -> toType(getMaxProperties(), type);
			case PROP_minProperties -> toType(getMinProperties(), type);
			case PROP_required -> toType(getRequired(), type);
			case PROP_enum -> toType(getEnum(), type);
			case PROP_type -> toType(getType(), type);
			case PROP_items -> toType(getItems(), type);
			case PROP_allOf -> toType(getAllOf(), type);
			case PROP_oneOf -> toType(getOneOf(), type);
			case PROP_anyOf -> toType(getAnyOf(), type);
			case PROP_properties -> toType(getProperties(), type);
			case PROP_additionalProperties -> toType(getAdditionalProperties(), type);
			case PROP_not -> toType(getNot(), type);
			case PROP_nullable -> toType(getNullable(), type);
			case PROP_deprecated -> toType(getDeprecated(), type);
			case PROP_discriminator -> toType(getDiscriminator(), type);
			case PROP_readOnly -> toType(getReadOnly(), type);
			case PROP_writeOnly -> toType(getWriteOnly(), type);
			case PROP_xml -> toType(getXml(), type);
			case PROP_externalDocs -> toType(getExternalDocs(), type);
			case PROP_example -> toType(getExample(), type);
			case PROP_ref -> toType(getRef(), type);
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
	public List<Object> getAllOf() { return nullIfEmpty(allOf); }

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getAnyOf() { return nullIfEmpty(anyOf); }

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
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() { return deprecated; }

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
	public Discriminator getDiscriminator() { return discriminator; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() { return nullIfEmpty(enum_); }

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
	 * Bean property getter:  <property>not</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getNot() { return not; }

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getNullable() { return nullable; }

	/**
	 * Bean property getter:  <property>allOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getOneOf() { return nullIfEmpty(oneOf); }

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
	public Map<String,SchemaInfo> getProperties() { return properties; }

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
	 * <p>
	 * The list of required properties.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<String> getRequired() { return nullIfEmpty(required); }

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
	 * Bean property getter:  <property>WriteOnly</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getWriteOnly() { return writeOnly; }

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
			.addIf(ne(anyOf), PROP_anyOf)
			.addIf(nn(default_), PROP_default)
			.addIf(nn(deprecated), PROP_deprecated)
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
			.addIf(nn(not), PROP_not)
			.addIf(nn(nullable), PROP_nullable)
			.addIf(ne(oneOf), PROP_oneOf)
			.addIf(nn(pattern), PROP_pattern)
			.addIf(nn(properties), PROP_properties)
			.addIf(nn(readOnly), PROP_readOnly)
			.addIf(ne(required), PROP_required)
			.addIf(nn(title), PROP_title)
			.addIf(nn(type), PROP_type)
			.addIf(nn(uniqueItems), PROP_uniqueItems)
			.addIf(nn(writeOnly), PROP_writeOnly)
			.addIf(nn(xml), PROP_xml)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param openApi The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public SchemaInfo resolveRefs(OpenApi openApi, Deque<String> refStack, int maxDepth) {

		if (nn(ref)) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = openApi.findRef(ref, SchemaInfo.class);
			r = r.resolveRefs(openApi, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (nn(items))
			items = items.resolveRefs(openApi, refStack, maxDepth);

		if (nn(properties))
			for (var e : properties.entrySet())
				e.setValue(e.getValue().resolveRefs(openApi, refStack, maxDepth));

		if (nn(additionalProperties))
			additionalProperties = additionalProperties.resolveRefs(openApi, refStack, maxDepth);

		this.example = null;

		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public SchemaInfo set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_ref -> setRef(value);
			case PROP_additionalProperties -> setAdditionalProperties(toType(value, SchemaInfo.class));
			case PROP_allOf -> setAllOf(listb(Object.class).addAny(value).sparse().build());
			case PROP_anyOf -> setAnyOf(listb(Object.class).addAny(value).sparse().build());
			case PROP_default -> setDefault(value);
			case PROP_deprecated -> setDeprecated(toBoolean(value));
			case PROP_description -> setDescription(s(value));
			case PROP_discriminator -> setDiscriminator(toType(value, Discriminator.class));
			case PROP_enum -> setEnum(listb(Object.class).addAny(value).sparse().build());
			case PROP_example -> setExample(value);
			case PROP_exclusiveMaximum -> setExclusiveMaximum(toBoolean(value));
			case PROP_exclusiveMinimum -> setExclusiveMinimum(toBoolean(value));
			case PROP_externalDocs -> setExternalDocs(toType(value, ExternalDocumentation.class));
			case PROP_format -> setFormat(s(value));
			case PROP_items -> setItems(toType(value, Items.class));
			case PROP_maxItems -> setMaxItems(toInteger(value));
			case PROP_maxLength -> setMaxLength(toInteger(value));
			case PROP_maxProperties -> setMaxProperties(toInteger(value));
			case PROP_maximum -> setMaximum(toNumber(value));
			case PROP_minItems -> setMinItems(toInteger(value));
			case PROP_minLength -> setMinLength(toInteger(value));
			case PROP_minProperties -> setMinProperties(toInteger(value));
			case PROP_minimum -> setMinimum(toNumber(value));
			case PROP_multipleOf -> setMultipleOf(toNumber(value));
			case PROP_not -> setNot(toType(value, SchemaInfo.class));
			case PROP_nullable -> setNullable(toBoolean(value));
			case PROP_oneOf -> setOneOf(listb(Object.class).addAny(value).sparse().build());
			case PROP_pattern -> setPattern(s(value));
			case PROP_properties -> setProperties(toMapBuilder(value, String.class, SchemaInfo.class).sparse().build());
			case PROP_readOnly -> setReadOnly(toBoolean(value));
			case PROP_required -> setRequired(listb(String.class).addAny(value).sparse().build());
			case PROP_title -> setTitle(s(value));
			case PROP_type -> setType(s(value));
			case PROP_uniqueItems -> setUniqueItems(toBoolean(value));
			case PROP_writeOnly -> setWriteOnly(toBoolean(value));
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
	 * @return This object
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
	 * @return This object
	 */
	public SchemaInfo setAllOf(Collection<Object> value) {
		allOf.clear();
		if (nn(value))
			allOf.addAll(value);
		return this;
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
		anyOf.clear();
		if (nn(value))
			anyOf.addAll(value);
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
	 * @return This object
	 */
	public SchemaInfo setDefault(Object value) {
		default_ = value;
		return this;
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
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setEnum(Collection<Object> value) {
		enum_.clear();
		if (nn(value))
			enum_.addAll(value);
		return this;
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
	 * Bean property setter:  <property>allOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setOneOf(Collection<Object> value) {
		oneOf.clear();
		if (nn(value))
			oneOf.addAll(value);
		return this;
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
	 * Bean property setter:  <property>properties</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public SchemaInfo setProperties(Map<String,SchemaInfo> value) {
		properties = copyOf(value);
		return this;
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
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Beanp("$ref")
	public SchemaInfo setRef(Object value) {
		ref = s(value);
		return this;
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
		required.clear();
		if (nn(value))
			required.addAll(value);
		return this;
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

	@Override /* Overridden from OpenApiElement */
	public SchemaInfo strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public SchemaInfo strict(Object value) {
		super.strict(value);
		return this;
	}
}