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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;

/**
 * Describes a single operation parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.OpenApi">Overview &gt; juneau-rest-server &gt; OpenAPI</a>
 * </ul>
 */
@Bean(properties="name,in,description,required,deprecated,allowEmptyValue,style,explode,allowReserved,schema,example,examples,*")
@FluentSetters
public class Parameter extends OpenApiElement {

	private static final String[] VALID_IN = {"query", "header", "path", "cookie"};
	private static final String[] VALID_STYLES = {"matrix", "label", "form", "simple", "spaceDelimited", "pipeDelimited", "deepObject"};

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
	 * Returns the parameter name.
	 *
	 * @return The parameter name.
	 */
	public String getName() {
		return name;
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
	 * Returns the parameter location.
	 *
	 * @return The parameter location.
	 */
	public String getIn() {
		return in;
	}

	/**
	 * Sets the parameter location.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setIn(String value) {
		if (isStrict() && ! contains(value, VALID_IN))
			throw new BasicRuntimeException(
				"Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_IN)
			);
		this.in = value;
		return this;
	}

	/**
	 * Returns the description.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return description;
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
	 * Returns the style.
	 *
	 * @return The style.
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Sets the style.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public Parameter setStyle(String value) {
		if (isStrict() && ! contains(value, VALID_STYLES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setStyle(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_STYLES)
			);
		this.style = value;
		return this;
	}

	/**
	 * Returns the required flag.
	 *
	 * @return The required flag.
	 */
	public Boolean getRequired() {
		return required;
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
	 * Returns the deprecated flag.
	 *
	 * @return The deprecated flag.
	 */
	public Boolean getDeprecated() {
		return deprecated;
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
	 * Returns the allow empty value flag.
	 *
	 * @return The allow empty value flag.
	 */
	public Boolean getAllowEmptyValue() {
		return allowEmptyValue;
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
	 * Returns the explode flag.
	 *
	 * @return The explode flag.
	 */
	public Boolean getExplode() {
		return explode;
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
	 * Returns the allow reserved flag.
	 *
	 * @return The allow reserved flag.
	 */
	public Boolean getAllowReserved() {
		return allowReserved;
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
	 * Returns the schema.
	 *
	 * @return The schema.
	 */
	public SchemaInfo getSchema() {
		return schema;
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
	 * Returns the example.
	 *
	 * @return The example.
	 */
	public Object getExample() {
		return example;
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
	 * Returns the examples map.
	 *
	 * @return The examples map.
	 */
	public Map<String,Example> getExamples() {
		return examples;
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
}
