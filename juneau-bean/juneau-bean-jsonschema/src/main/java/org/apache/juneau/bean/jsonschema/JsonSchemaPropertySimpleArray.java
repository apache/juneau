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
		setItems(new JsonSchema().setType(elementType));
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAdditionalItems(JsonSchema...value) {
		super.addAdditionalItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAllOf(JsonSchema...value) {
		super.addAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addAnyOf(JsonSchema...value) {
		super.addAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDef(String name, JsonSchema value) {
		super.addDef(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDefinition(String name, JsonSchema value) {
		super.addDefinition(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependency(String name, JsonSchema value) {
		super.addDependency(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependentRequired(String name, List<String> value) {
		super.addDependentRequired(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addDependentSchema(String name, JsonSchema value) {
		super.addDependentSchema(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addEnum(Object...value) {
		super.addEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addExamples(Object...value) {
		super.addExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addItems(JsonSchema...value) {
		super.addItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addOneOf(JsonSchema...value) {
		super.addOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addPatternProperties(JsonSchemaProperty...value) {
		super.addPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addPrefixItems(JsonSchema...value) {
		super.addPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addProperties(JsonSchema...value) {
		super.addProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(JsonSchemaProperty...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(List<String> value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addRequired(String...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray addTypes(JsonType...value) {
		super.addTypes(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setAllOf(List<JsonSchema> value) {
		super.setAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setAnyOf(List<JsonSchema> value) {
		super.setAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setConst(Object value) {
		super.setConst(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setContentMediaType(String value) {
		super.setContentMediaType(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDefinitions(Map<String,JsonSchema> value) {
		super.setDefinitions(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependencies(Map<String,JsonSchema> value) {
		super.setDependencies(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependentRequired(Map<String,List<String>> value) {
		super.setDependentRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDependentSchemas(Map<String,JsonSchema> value) {
		super.setDependentSchemas(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setDescription(String value) {
		super.setDescription(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setEnum(List<Object> value) {
		super.setEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExamples(List<Object> value) {
		super.setExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExclusiveMaximum(Number value) {
		super.setExclusiveMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setExclusiveMinimum(Number value) {
		super.setExclusiveMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	@Deprecated(since = "10.0", forRemoval = true) // Parent method is deprecated
	public JsonSchemaPropertySimpleArray setId(Object value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaximum(Number value) {
		super.setMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxItems(Integer value) {
		super.setMaxItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxLength(Integer value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMaxProperties(Integer value) {
		super.setMaxProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinimum(Number value) {
		super.setMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinItems(Integer value) {
		super.setMinItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinLength(Integer value) {
		super.setMinLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMinProperties(Integer value) {
		super.setMinProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setMultipleOf(Number value) {
		super.setMultipleOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setName(String value) {
		super.setName(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setNot(JsonSchema value) {
		super.setNot(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setOneOf(List<JsonSchema> value) {
		super.setOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPattern(String value) {
		super.setPattern(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPatternProperties(Map<String,JsonSchema> value) {
		super.setPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setPrefixItems(JsonSchemaArray value) {
		super.setPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setProperties(Map<String,JsonSchema> value) {
		super.setProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setReadOnly(Boolean value) {
		super.setReadOnly(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setRequired(List<String> value) {
		super.setRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setSchemaMap(JsonSchemaMap value) {
		super.setSchemaMap(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setType(Object value) {
		super.setType(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUnevaluatedItems(JsonSchema value) {
		super.setUnevaluatedItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUnevaluatedProperties(JsonSchema value) {
		super.setUnevaluatedProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setUniqueItems(Boolean value) {
		super.setUniqueItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchemaProperty */
	public JsonSchemaPropertySimpleArray setWriteOnly(Boolean value) {
		super.setWriteOnly(value);
		return this;
	}
}