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
 * Represents a JSON property in the JSON-Schema core specification.
 */
public class JsonSchemaProperty extends JsonSchema {

	/**
	 * Default constructor.
	 */
	public JsonSchemaProperty() {}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.
	 */
	public JsonSchemaProperty(String name) {
		setName(name);
	}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.
	 * @param type The JSON type of this property.
	 */
	public JsonSchemaProperty(String name, JsonType type) {
		setName(name);
		setType(type);
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setName(String name) {
		super.setName(name);
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setId(Object id) {
		super.setId(id);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setTitle(String title) {
		super.setTitle(title);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDescription(String description) {
		super.setDescription(description);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setType(Object type) {
		super.setType(type);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addTypes(JsonType...types) {
		super.addTypes(types);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDefinitions(Map<String,JsonSchema> definitions) {
		super.setDefinitions(definitions);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDefinition(String name, JsonSchema definition) {
		super.addDefinition(name, definition);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setProperties(Map<String,JsonSchema> properties) {
		super.setProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addProperties(JsonSchema...properties) {
		super.addProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPatternProperties(Map<String,JsonSchema> patternProperties) {
		super.setPatternProperties(patternProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addPatternProperties(JsonSchemaProperty...properties) {
		super.addPatternProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependencies(Map<String,JsonSchema> dependencies) {
		super.setDependencies(dependencies);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependency(String name, JsonSchema dependency) {
		super.addDependency(name, dependency);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addItems(JsonSchema...items) {
		super.addItems(items);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMultipleOf(Number multipleOf) {
		super.setMultipleOf(multipleOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaximum(Number maximum) {
		super.setMaximum(maximum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExclusiveMaximum(Number exclusiveMaximum) {
		super.setExclusiveMaximum(exclusiveMaximum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinimum(Number minimum) {
		super.setMinimum(minimum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExclusiveMinimum(Number exclusiveMinimum) {
		super.setExclusiveMinimum(exclusiveMinimum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxLength(Integer maxLength) {
		super.setMaxLength(maxLength);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinLength(Integer minLength) {
		super.setMinLength(minLength);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPattern(String pattern) {
		super.setPattern(pattern);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addAdditionalItems(JsonSchema...additionalItems) {
		super.addAdditionalItems(additionalItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxItems(Integer maxItems) {
		super.setMaxItems(maxItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinItems(Integer minItems) {
		super.setMinItems(minItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUniqueItems(Boolean uniqueItems) {
		super.setUniqueItems(uniqueItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxProperties(Integer maxProperties) {
		super.setMaxProperties(maxProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinProperties(Integer minProperties) {
		super.setMinProperties(minProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setRequired(List<String> required) {
		super.setRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(List<String> required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(String...required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(JsonSchemaProperty...properties) {
		super.addRequired(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setEnum(List<Object> _enum) {
		super.setEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addEnum(Object..._enum) {
		super.addEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setAllOf(List<JsonSchema> allOf) {
		super.setAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addAllOf(JsonSchema...allOf) {
		super.addAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setAnyOf(List<JsonSchema> anyOf) {
		super.setAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addAnyOf(JsonSchema...anyOf) {
		super.addAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setOneOf(List<JsonSchema> oneOf) {
		super.setOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addOneOf(JsonSchema...oneOf) {
		super.addOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema getNot() { return super.getNot(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setNot(JsonSchema not) {
		super.setNot(not);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setConst(Object _const) {
		super.setConst(_const);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExamples(List<Object> examples) {
		super.setExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addExamples(Object...examples) {
		super.addExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setReadOnly(Boolean readOnly) {
		super.setReadOnly(readOnly);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setWriteOnly(Boolean writeOnly) {
		super.setWriteOnly(writeOnly);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setContentMediaType(String contentMediaType) {
		super.setContentMediaType(contentMediaType);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setContentEncoding(String contentEncoding) {
		super.setContentEncoding(contentEncoding);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDef(String name, JsonSchema def) {
		super.addDef(name, def);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPrefixItems(JsonSchemaArray prefixItems) {
		super.setPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addPrefixItems(JsonSchema...prefixItems) {
		super.addPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema getUnevaluatedItems() { return super.getUnevaluatedItems(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUnevaluatedItems(JsonSchema unevaluatedItems) {
		super.setUnevaluatedItems(unevaluatedItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUnevaluatedProperties(JsonSchema unevaluatedProperties) {
		super.setUnevaluatedProperties(unevaluatedProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependentSchemas(Map<String,JsonSchema> dependentSchemas) {
		super.setDependentSchemas(dependentSchemas);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependentSchema(String name, JsonSchema schema) {
		super.addDependentSchema(name, schema);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependentRequired(Map<String,List<String>> dependentRequired) {
		super.setDependentRequired(dependentRequired);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependentRequired(String name, List<String> required) {
		super.addDependentRequired(name, required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema resolve() {
		return super.resolve();
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setSchemaMap(JsonSchemaMap schemaMap) {
		super.setSchemaMap(schemaMap);
		return this;
	}
}