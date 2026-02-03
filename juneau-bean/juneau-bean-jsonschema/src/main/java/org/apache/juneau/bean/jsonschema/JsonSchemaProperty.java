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
	public JsonSchemaProperty addAdditionalItems(JsonSchema...value) {
		super.addAdditionalItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addAllOf(JsonSchema...value) {
		super.addAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addAnyOf(JsonSchema...value) {
		super.addAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDef(String name, JsonSchema value) {
		super.addDef(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDefinition(String name, JsonSchema value) {
		super.addDefinition(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependency(String name, JsonSchema value) {
		super.addDependency(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependentRequired(String name, List<String> value) {
		super.addDependentRequired(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addDependentSchema(String name, JsonSchema value) {
		super.addDependentSchema(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addEnum(Object...value) {
		super.addEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addExamples(Object...value) {
		super.addExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addItems(JsonSchema...value) {
		super.addItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addOneOf(JsonSchema...value) {
		super.addOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addPatternProperties(JsonSchemaProperty...value) {
		super.addPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addPrefixItems(JsonSchema...value) {
		super.addPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addProperties(JsonSchema...value) {
		super.addProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(JsonSchemaProperty...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(List<String> value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addRequired(String...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty addTypes(JsonType...value) {
		super.addTypes(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema getNot() { return super.getNot(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchema getUnevaluatedItems() { return super.getUnevaluatedItems(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchema resolve() {
		return super.resolve();
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setAllOf(List<JsonSchema> value) {
		super.setAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setAnyOf(List<JsonSchema> value) {
		super.setAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setConst(Object value) {
		super.setConst(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setContentMediaType(String value) {
		super.setContentMediaType(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDefinitions(Map<String,JsonSchema> value) {
		super.setDefinitions(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependencies(Map<String,JsonSchema> value) {
		super.setDependencies(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependentRequired(Map<String,List<String>> value) {
		super.setDependentRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDependentSchemas(Map<String,JsonSchema> value) {
		super.setDependentSchemas(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setDescription(String value) {
		super.setDescription(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setEnum(List<Object> value) {
		super.setEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExamples(List<Object> value) {
		super.setExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExclusiveMaximum(Number value) {
		super.setExclusiveMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setExclusiveMinimum(Number value) {
		super.setExclusiveMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	@SuppressWarnings({"java:S1186","removal"})
	public JsonSchemaProperty setId(Object value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaximum(Number value) {
		super.setMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxItems(Integer value) {
		super.setMaxItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxLength(Integer value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMaxProperties(Integer value) {
		super.setMaxProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinimum(Number value) {
		super.setMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinItems(Integer value) {
		super.setMinItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinLength(Integer value) {
		super.setMinLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMinProperties(Integer value) {
		super.setMinProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setMultipleOf(Number value) {
		super.setMultipleOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setName(String value) {
		super.setName(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setNot(JsonSchema value) {
		super.setNot(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setOneOf(List<JsonSchema> value) {
		super.setOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPattern(String value) {
		super.setPattern(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPatternProperties(Map<String,JsonSchema> value) {
		super.setPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setPrefixItems(JsonSchemaArray value) {
		super.setPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setProperties(Map<String,JsonSchema> value) {
		super.setProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setReadOnly(Boolean value) {
		super.setReadOnly(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setRequired(List<String> value) {
		super.setRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setSchemaMap(JsonSchemaMap value) {
		super.setSchemaMap(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setType(Object value) {
		super.setType(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUnevaluatedItems(JsonSchema value) {
		super.setUnevaluatedItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUnevaluatedProperties(JsonSchema value) {
		super.setUnevaluatedProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setUniqueItems(Boolean value) {
		super.setUniqueItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaProperty setWriteOnly(Boolean value) {
		super.setWriteOnly(value);
		return this;
	}
}