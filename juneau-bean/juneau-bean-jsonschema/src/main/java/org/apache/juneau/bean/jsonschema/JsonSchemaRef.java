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

import java.net.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Convenience class for representing a schema reference such as <js>"{'$ref':'/url/to/ref'}"</js>.
 *
 * <p>
 * An instance of this object is equivalent to calling...
 *
 * <p class='bjava'>
 * 	JsonSchema <jv>schema</jv> = <jk>new</jk> JsonSchema().setRef(<jv>uri</jv>);
 * </p>
 */
public class JsonSchemaRef extends JsonSchema {

	/**
	 * Constructor.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the target reference.  Can be <jk>null</jk>.
	 */
	public JsonSchemaRef(Object uri) {
		this.setRef(uri);
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAdditionalItems(JsonSchema...value) {
		super.addAdditionalItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAllOf(JsonSchema...value) {
		super.addAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAnyOf(JsonSchema...value) {
		super.addAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDef(String name, JsonSchema value) {
		super.addDef(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDefinition(String name, JsonSchema value) {
		super.addDefinition(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependency(String name, JsonSchema value) {
		super.addDependency(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependentRequired(String name, List<String> value) {
		super.addDependentRequired(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependentSchema(String name, JsonSchema value) {
		super.addDependentSchema(name, value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addEnum(Object...value) {
		super.addEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addExamples(Object...value) {
		super.addExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addItems(JsonSchema...value) {
		super.addItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addOneOf(JsonSchema...value) {
		super.addOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addPatternProperties(JsonSchemaProperty...value) {
		super.addPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addPrefixItems(JsonSchema...value) {
		super.addPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addProperties(JsonSchema...value) {
		super.addProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(JsonSchemaProperty...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(List<String> value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(String...value) {
		super.addRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addTypes(JsonType...value) {
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
	public JsonSchemaRef setAllOf(List<JsonSchema> value) {
		super.setAllOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setAnyOf(List<JsonSchema> value) {
		super.setAnyOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setConst(Object value) {
		super.setConst(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setContentMediaType(String value) {
		super.setContentMediaType(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDefinitions(Map<String,JsonSchema> value) {
		super.setDefinitions(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependencies(Map<String,JsonSchema> value) {
		super.setDependencies(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependentRequired(Map<String,List<String>> value) {
		super.setDependentRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependentSchemas(Map<String,JsonSchema> value) {
		super.setDependentSchemas(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDescription(String value) {
		super.setDescription(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setEnum(List<Object> value) {
		super.setEnum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExamples(List<Object> value) {
		super.setExamples(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExclusiveMaximum(Number value) {
		super.setExclusiveMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExclusiveMinimum(Number value) {
		super.setExclusiveMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	@SuppressWarnings({"java:S1186","removal"})
	public JsonSchemaRef setId(Object value) {
		super.setId(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaximum(Number value) {
		super.setMaximum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxItems(Integer value) {
		super.setMaxItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxLength(Integer value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxProperties(Integer value) {
		super.setMaxProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinimum(Number value) {
		super.setMinimum(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinItems(Integer value) {
		super.setMinItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinLength(Integer value) {
		super.setMinLength(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinProperties(Integer value) {
		super.setMinProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMultipleOf(Number value) {
		super.setMultipleOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setName(String value) {
		super.setName(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setNot(JsonSchema value) {
		super.setNot(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setOneOf(List<JsonSchema> value) {
		super.setOneOf(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPattern(String value) {
		super.setPattern(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPatternProperties(Map<String,JsonSchema> value) {
		super.setPatternProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPrefixItems(JsonSchemaArray value) {
		super.setPrefixItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setProperties(Map<String,JsonSchema> value) {
		super.setProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setReadOnly(Boolean value) {
		super.setReadOnly(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setRequired(List<String> value) {
		super.setRequired(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setSchemaMap(JsonSchemaMap value) {
		super.setSchemaMap(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setTitle(String value) {
		super.setTitle(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setType(Object value) {
		super.setType(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUnevaluatedItems(JsonSchema value) {
		super.setUnevaluatedItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUnevaluatedProperties(JsonSchema value) {
		super.setUnevaluatedProperties(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUniqueItems(Boolean value) {
		super.setUniqueItems(value);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setWriteOnly(Boolean value) {
		super.setWriteOnly(value);
		return this;
	}
}