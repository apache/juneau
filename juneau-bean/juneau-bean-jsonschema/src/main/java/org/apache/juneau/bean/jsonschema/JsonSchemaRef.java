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
	public JsonSchemaRef setName(String name) {
		super.setName(name);
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setId(Object id) {
		super.setId(id);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setTitle(String title) {
		super.setTitle(title);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDescription(String description) {
		super.setDescription(description);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setType(Object type) {
		super.setType(type);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addTypes(JsonType...types) {
		super.addTypes(types);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDefinitions(Map<String,JsonSchema> definitions) {
		super.setDefinitions(definitions);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDefinition(String name, JsonSchema definition) {
		super.addDefinition(name, definition);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setProperties(Map<String,JsonSchema> properties) {
		super.setProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addProperties(JsonSchema...properties) {
		super.addProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPatternProperties(Map<String,JsonSchema> patternProperties) {
		super.setPatternProperties(patternProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addPatternProperties(JsonSchemaProperty...properties) {
		super.addPatternProperties(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependencies(Map<String,JsonSchema> dependencies) {
		super.setDependencies(dependencies);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependency(String name, JsonSchema dependency) {
		super.addDependency(name, dependency);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addItems(JsonSchema...items) {
		super.addItems(items);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMultipleOf(Number multipleOf) {
		super.setMultipleOf(multipleOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaximum(Number maximum) {
		super.setMaximum(maximum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExclusiveMaximum(Number exclusiveMaximum) {
		super.setExclusiveMaximum(exclusiveMaximum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinimum(Number minimum) {
		super.setMinimum(minimum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExclusiveMinimum(Number exclusiveMinimum) {
		super.setExclusiveMinimum(exclusiveMinimum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxLength(Integer maxLength) {
		super.setMaxLength(maxLength);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinLength(Integer minLength) {
		super.setMinLength(minLength);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPattern(String pattern) {
		super.setPattern(pattern);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAdditionalItems(JsonSchema...additionalItems) {
		super.addAdditionalItems(additionalItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxItems(Integer maxItems) {
		super.setMaxItems(maxItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinItems(Integer minItems) {
		super.setMinItems(minItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUniqueItems(Boolean uniqueItems) {
		super.setUniqueItems(uniqueItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMaxProperties(Integer maxProperties) {
		super.setMaxProperties(maxProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setMinProperties(Integer minProperties) {
		super.setMinProperties(minProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setRequired(List<String> required) {
		super.setRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(List<String> required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(String...required) {
		super.addRequired(required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addRequired(JsonSchemaProperty...properties) {
		super.addRequired(properties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setEnum(List<Object> _enum) {
		super.setEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addEnum(Object..._enum) {
		super.addEnum(_enum);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setAllOf(List<JsonSchema> allOf) {
		super.setAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAllOf(JsonSchema...allOf) {
		super.addAllOf(allOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setAnyOf(List<JsonSchema> anyOf) {
		super.setAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addAnyOf(JsonSchema...anyOf) {
		super.addAnyOf(anyOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setOneOf(List<JsonSchema> oneOf) {
		super.setOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addOneOf(JsonSchema...oneOf) {
		super.addOneOf(oneOf);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema getNot() { return super.getNot(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setNot(JsonSchema not) {
		super.setNot(not);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setConst(Object _const) {
		super.setConst(_const);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setExamples(List<Object> examples) {
		super.setExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addExamples(Object...examples) {
		super.addExamples(examples);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setReadOnly(Boolean readOnly) {
		super.setReadOnly(readOnly);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setWriteOnly(Boolean writeOnly) {
		super.setWriteOnly(writeOnly);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setContentMediaType(String contentMediaType) {
		super.setContentMediaType(contentMediaType);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setContentEncoding(String contentEncoding) {
		super.setContentEncoding(contentEncoding);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDef(String name, JsonSchema def) {
		super.addDef(name, def);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setPrefixItems(JsonSchemaArray prefixItems) {
		super.setPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addPrefixItems(JsonSchema...prefixItems) {
		super.addPrefixItems(prefixItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema getUnevaluatedItems() { return super.getUnevaluatedItems(); }

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUnevaluatedItems(JsonSchema unevaluatedItems) {
		super.setUnevaluatedItems(unevaluatedItems);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setUnevaluatedProperties(JsonSchema unevaluatedProperties) {
		super.setUnevaluatedProperties(unevaluatedProperties);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependentSchemas(Map<String,JsonSchema> dependentSchemas) {
		super.setDependentSchemas(dependentSchemas);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependentSchema(String name, JsonSchema schema) {
		super.addDependentSchema(name, schema);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setDependentRequired(Map<String,List<String>> dependentRequired) {
		super.setDependentRequired(dependentRequired);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef addDependentRequired(String name, List<String> required) {
		super.addDependentRequired(name, required);
		return this;
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchema resolve() {
		return super.resolve();
	}

	@Override /* Overridden from JsonSchema */
	public JsonSchemaRef setSchemaMap(JsonSchemaMap schemaMap) {
		super.setSchemaMap(schemaMap);
		return this;
	}
}