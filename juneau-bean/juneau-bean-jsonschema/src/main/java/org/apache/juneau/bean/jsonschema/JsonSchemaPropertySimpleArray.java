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

import java.util.*;

/**
 * Convenience class for representing a property that's an array of simple types.
 *
 * <p>
 * An instance of this object is equivalent to calling...
 *
 * <p class='bjava'>
 * 	JsonSchemaProperty <jv>property</jv> = <jk>new</jk> JsonSchemaProperty(<jv>name</jv>)
 * 		.setType(JsonType.<jsf>ARRAY</jsf>)
 * 		.setItems(
 * 			<jk>new</jk> JsonSchema().setType(<jv>elementType</jv>)
 * 		);
 * </p>
 */
public class JsonSchemaPropertySimpleArray extends JsonSchemaProperty {

	/**
	 * Constructor.
	 *
	 * @param name The name of the schema property.
	 * @param elementType The JSON type of the elements in the array.
	 */
	public JsonSchemaPropertySimpleArray(String name, JsonType elementType) {
		setName(name);
		setType(JsonType.ARRAY);
		setItems(
			new JsonSchema().setType(elementType)
		);
	}


	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setName(String name) {
		super.setName(name);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setId(Object id) {
		super.setId(id);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setTitle(String title) {
		super.setTitle(title);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDescription(String description) {
		super.setDescription(description);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setType(Object type) {
		super.setType(type);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addTypes(JsonType...types) {
		super.addTypes(types);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDefinitions(Map<String,JsonSchema> definitions) {
		super.setDefinitions(definitions);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDefinition(String name, JsonSchema definition) {
		super.addDefinition(name, definition);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setProperties(Map<String,JsonSchema> properties) {
		super.setProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addProperties(JsonSchema...properties) {
		super.addProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPatternProperties(Map<String,JsonSchema> patternProperties) {
		super.setPatternProperties(patternProperties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addPatternProperties(JsonSchemaProperty...properties) {
		super.addPatternProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependencies(Map<String,JsonSchema> dependencies) {
		super.setDependencies(dependencies);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependency(String name, JsonSchema dependency) {
		super.addDependency(name, dependency);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addItems(JsonSchema...items) {
		super.addItems(items);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMultipleOf(Number multipleOf) {
		super.setMultipleOf(multipleOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaximum(Number maximum) {
		super.setMaximum(maximum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExclusiveMaximum(Number exclusiveMaximum) {
		super.setExclusiveMaximum(exclusiveMaximum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinimum(Number minimum) {
		super.setMinimum(minimum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExclusiveMinimum(Number exclusiveMinimum) {
		super.setExclusiveMinimum(exclusiveMinimum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxLength(Integer maxLength) {
		super.setMaxLength(maxLength);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinLength(Integer minLength) {
		super.setMinLength(minLength);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPattern(String pattern) {
		super.setPattern(pattern);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAdditionalItems(JsonSchema...additionalItems) {
		super.addAdditionalItems(additionalItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxItems(Integer maxItems) {
		super.setMaxItems(maxItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinItems(Integer minItems) {
		super.setMinItems(minItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUniqueItems(Boolean uniqueItems) {
		super.setUniqueItems(uniqueItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxProperties(Integer maxProperties) {
		super.setMaxProperties(maxProperties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinProperties(Integer minProperties) {
		super.setMinProperties(minProperties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setRequired(List<String> required) {
		super.setRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(List<String> required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(String...required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(JsonSchemaProperty...properties) {
		super.addRequired(properties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setEnum(List<Object> _enum) {
		super.setEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addEnum(Object..._enum) {
		super.addEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setAllOf(List<JsonSchema> allOf) {
		super.setAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAllOf(JsonSchema...allOf) {
		super.addAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setAnyOf(List<JsonSchema> anyOf) {
		super.setAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAnyOf(JsonSchema...anyOf) {
		super.addAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setOneOf(List<JsonSchema> oneOf) {
		super.setOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addOneOf(JsonSchema...oneOf) {
		super.addOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setNot(JsonSchema not) {
		super.setNot(not);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setConst(Object _const) {
		super.setConst(_const);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExamples(List<Object> examples) {
		super.setExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addExamples(Object...examples) {
		super.addExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setReadOnly(Boolean readOnly) {
		super.setReadOnly(readOnly);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setWriteOnly(Boolean writeOnly) {
		super.setWriteOnly(writeOnly);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setContentMediaType(String contentMediaType) {
		super.setContentMediaType(contentMediaType);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setContentEncoding(String contentEncoding) {
		super.setContentEncoding(contentEncoding);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDef(String name, JsonSchema def) {
		super.addDef(name, def);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPrefixItems(JsonSchemaArray prefixItems) {
		super.setPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addPrefixItems(JsonSchema...prefixItems) {
		super.addPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUnevaluatedItems(JsonSchema unevaluatedItems) {
		super.setUnevaluatedItems(unevaluatedItems);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUnevaluatedProperties(JsonSchema unevaluatedProperties) {
		super.setUnevaluatedProperties(unevaluatedProperties);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependentSchemas(Map<String,JsonSchema> dependentSchemas) {
		super.setDependentSchemas(dependentSchemas);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependentSchema(String name, JsonSchema schema) {
		super.addDependentSchema(name, schema);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependentRequired(Map<String,List<String>> dependentRequired) {
		super.setDependentRequired(dependentRequired);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependentRequired(String name, List<String> required) {
		super.addDependentRequired(name, required);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setSchemaMap(JsonSchemaMap schemaMap) {
		super.setSchemaMap(schemaMap);
		return this;
	}
}