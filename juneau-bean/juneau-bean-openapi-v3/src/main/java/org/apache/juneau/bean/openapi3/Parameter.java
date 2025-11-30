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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.containsAny;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.marshaller.*;

/**
 * Describes a single operation parameter.
 *
 * <p>
 * The Parameter Object describes a single parameter used in an API operation. Parameters can be passed in various
 * locations including the path, query string, headers, or cookies. Each parameter has a name, location, and schema
 * that defines its type and constraints.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Parameter Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string, REQUIRED) - The name of the parameter
 * 	<li><c>in</c> (string, REQUIRED) - The location of the parameter. Possible values: <js>"query"</js>, <js>"header"</js>, <js>"path"</js>, or <js>"cookie"</js>
 * 	<li><c>description</c> (string) - A brief description of the parameter (CommonMark syntax may be used)
 * 	<li><c>required</c> (boolean) - Determines whether this parameter is mandatory (must be <jk>true</jk> if <c>in</c> is <js>"path"</js>)
 * 	<li><c>deprecated</c> (boolean) - Specifies that a parameter is deprecated
 * 	<li><c>allowEmptyValue</c> (boolean) - Sets the ability to pass empty-valued parameters (valid only for <js>"query"</js> parameters)
 * 	<li><c>style</c> (string) - Describes how the parameter value will be serialized
 * 	<li><c>explode</c> (boolean) - When true, parameter values of type array or object generate separate parameters for each value
 * 	<li><c>allowReserved</c> (boolean) - Determines whether the parameter value should allow reserved characters
 * 	<li><c>schema</c> ({@link SchemaInfo}) - The schema defining the type used for the parameter
 * 	<li><c>example</c> (any) - Example of the parameter's potential value
 * 	<li><c>examples</c> (map of {@link Example}) - Examples of the parameter's potential value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a query parameter</jc>
 * 	Parameter <jv>param</jv> = <jk>new</jk> Parameter()
 * 		.setName(<js>"status"</js>)
 * 		.setIn(<js>"query"</js>)
 * 		.setDescription(<js>"Status values to filter by"</js>)
 * 		.setRequired(<jk>false</jk>)
 * 		.setSchema(
 * 			<jk>new</jk> SchemaInfo()
 * 				.setType(<js>"array"</js>)
 * 				.setItems(
 * 					<jk>new</jk> Items().setType(<js>"string"</js>)
 * 				)
 * 		)
 * 		.setStyle(<js>"form"</js>)
 * 		.setExplode(<jk>true</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#parameter-object">OpenAPI Specification &gt; Parameter Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-parameters/">OpenAPI Describing Parameters</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Parameter extends OpenApiElement {

	private static final String[] VALID_IN = { "query", "header", "path", "cookie" };
	private static final String[] VALID_STYLES = { "matrix", "label", "form", "simple", "spaceDelimited", "pipeDelimited", "deepObject" };

	private String name, in, description, style;
	private Boolean required, deprecated, allowEmptyValue, explode, allowReserved;
	private SchemaInfo schema;
	private Object example;
	private Map<String,Example> examples;

	/**
	 * Default constructor.
	 */
	public Parameter() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Parameter(Parameter copyFrom) {
		super(copyFrom);
		this.name = copyFrom.name;
		this.in = copyFrom.in;
		this.description = copyFrom.description;
		this.style = copyFrom.style;
		this.required = copyFrom.required;
		this.deprecated = copyFrom.deprecated;
		this.allowEmptyValue = copyFrom.allowEmptyValue;
		this.explode = copyFrom.explode;
		this.allowReserved = copyFrom.allowReserved;
		this.schema = copyFrom.schema;
		this.example = copyFrom.example;
		this.examples = copyOf(copyFrom.examples);
	}

	/**
	 * Makes a copy of this object.
	 *
	 * @return A new copy of this object.
	 */
	public Parameter copy() {
		return new Parameter(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "in" -> toType(getIn(), type);
			case "description" -> toType(getDescription(), type);
			case "required" -> toType(getRequired(), type);
			case "deprecated" -> toType(getDeprecated(), type);
			case "allowEmptyValue" -> toType(getAllowEmptyValue(), type);
			case "style" -> toType(getStyle(), type);
			case "explode" -> toType(getExplode(), type);
			case "allowReserved" -> toType(getAllowReserved(), type);
			case "schema" -> toType(getSchema(), type);
			case "example" -> toType(getExample(), type);
			case "examples" -> toType(getExamples(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Returns the allow empty value flag.
	 *
	 * @return The allow empty value flag.
	 */
	public Boolean getAllowEmptyValue() { return allowEmptyValue; }

	/**
	 * Returns the allow reserved flag.
	 *
	 * @return The allow reserved flag.
	 */
	public Boolean getAllowReserved() { return allowReserved; }

	/**
	 * Returns the deprecated flag.
	 *
	 * @return The deprecated flag.
	 */
	public Boolean getDeprecated() { return deprecated; }

	/**
	 * Returns the description.
	 *
	 * @return The description.
	 */
	public String getDescription() { return description; }

	/**
	 * Returns the example.
	 *
	 * @return The example.
	 */
	public Object getExample() { return example; }

	/**
	 * Returns the examples map.
	 *
	 * @return The examples map.
	 */
	public Map<String,Example> getExamples() { return examples; }

	/**
	 * Returns the explode flag.
	 *
	 * @return The explode flag.
	 */
	public Boolean getExplode() { return explode; }

	/**
	 * Returns the parameter location.
	 *
	 * @return The parameter location.
	 */
	public String getIn() { return in; }

	/**
	 * Returns the parameter name.
	 *
	 * @return The parameter name.
	 */
	public String getName() { return name; }

	/**
	 * Returns the required flag.
	 *
	 * @return The required flag.
	 */
	public Boolean getRequired() { return required; }

	/**
	 * Returns the schema.
	 *
	 * @return The schema.
	 */
	public SchemaInfo getSchema() { return schema; }

	/**
	 * Returns the style.
	 *
	 * @return The style.
	 */
	public String getStyle() { return style; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(allowEmptyValue), "allowEmptyValue")
			.addIf(nn(allowReserved), "allowReserved")
			.addIf(nn(description), "description")
			.addIf(nn(deprecated), "deprecated")
			.addIf(nn(example), "example")
			.addIf(nn(examples), "examples")
			.addIf(nn(explode), "explode")
			.addIf(nn(in), "in")
			.addIf(nn(name), "name")
			.addIf(nn(required), "required")
			.addIf(nn(schema), "schema")
			.addIf(nn(style), "style")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Parameter set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "allowEmptyValue" -> setAllowEmptyValue(toType(value, Boolean.class));
			case "allowReserved" -> setAllowReserved(toType(value, Boolean.class));
			case "description" -> setDescription(s(value));
			case "deprecated" -> setDeprecated(toType(value, Boolean.class));
			case "example" -> setExample(value);
			case "examples" -> setExamples(toMapBuilder(value, String.class, Example.class).sparse().build());
			case "explode" -> setExplode(toType(value, Boolean.class));
			case "in" -> setIn(s(value));
			case "name" -> setName(s(value));
			case "required" -> setRequired(toType(value, Boolean.class));
			case "schema" -> setSchema(toType(value, SchemaInfo.class));
			case "style" -> setStyle(s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Sets the allow empty value flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setAllowEmptyValue(Boolean value) {
		this.allowEmptyValue = value;
		return this;
	}

	/**
	 * Sets the allow reserved flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setAllowReserved(Boolean value) {
		this.allowReserved = value;
		return this;
	}

	/**
	 * Sets the deprecated flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setDeprecated(Boolean value) {
		this.deprecated = value;
		return this;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setDescription(String value) {
		this.description = value;
		return this;
	}

	/**
	 * Sets the example.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setExample(Object value) {
		this.example = value;
		return this;
	}

	/**
	 * Sets the examples map.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setExamples(Map<String,Example> value) {
		this.examples = value;
		return this;
	}

	/**
	 * Sets the explode flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setExplode(Boolean value) {
		this.explode = value;
		return this;
	}

	/**
	 * Sets the parameter location.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setIn(String value) {
		if (isStrict() && ! containsAny(value, VALID_IN))
			throw rex("Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_IN));
		this.in = value;
		return this;
	}

	/**
	 * Sets the parameter name.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setName(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the required flag.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setRequired(Boolean value) {
		this.required = value;
		return this;
	}

	/**
	 * Sets the schema.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setSchema(SchemaInfo value) {
		this.schema = value;
		return this;
	}

	/**
	 * Sets the style.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setStyle(String value) {
		if (isStrict() && ! containsAny(value, VALID_STYLES))
			throw rex("Invalid value passed in to setStyle(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_STYLES));
		this.style = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Parameter strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Parameter strict(Object value) {
		super.strict(value);
		return this;
	}
}