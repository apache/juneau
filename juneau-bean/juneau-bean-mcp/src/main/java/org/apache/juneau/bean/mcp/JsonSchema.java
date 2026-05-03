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
package org.apache.juneau.bean.mcp;

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * JSON Schema subset used by MCP tool {@code inputSchema} objects.
 */
@Bean
public class JsonSchema {

	private String type;
	private Map<String, JsonSchema> properties;
	private List<String> required;
	private Object additionalProperties;
	private JsonSchema items;

	@Beanp(name = "$defs")
	private Map<String, JsonSchema> defs;

	/**
	 * Schema {@code type} keyword (for example {@code object}).
	 *
	 * @return The type, or {@code null} if not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the schema type keyword.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setType(String value) {
		type = value;
		return this;
	}

	/**
	 * Object property schemas.
	 *
	 * @return The properties map, or {@code null} if not set.
	 */
	public Map<String, JsonSchema> getProperties() {
		return properties;
	}

	/**
	 * Sets object property schemas.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setProperties(Map<String, JsonSchema> value) {
		properties = value;
		return this;
	}

	/**
	 * Required property names.
	 *
	 * @return The required list, or {@code null} if not set.
	 */
	public List<String> getRequired() {
		return required;
	}

	/**
	 * Sets required property names.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setRequired(List<String> value) {
		required = value;
		return this;
	}

	/**
	 * {@code additionalProperties} keyword (boolean or nested schema).
	 *
	 * @return The value, or {@code null} if not set.
	 */
	public Object getAdditionalProperties() {
		return additionalProperties;
	}

	/**
	 * Sets {@code additionalProperties}.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setAdditionalProperties(Object value) {
		additionalProperties = value;
		return this;
	}

	/**
	 * Array {@code items} schema.
	 *
	 * @return The items schema, or {@code null} if not set.
	 */
	public JsonSchema getItems() {
		return items;
	}

	/**
	 * Sets array items schema.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setItems(JsonSchema value) {
		items = value;
		return this;
	}

	/**
	 * {@code $defs} map for reusable sub-schemas.
	 *
	 * @return The defs map, or {@code null} if not set.
	 */
	public Map<String, JsonSchema> getDefs() {
		return defs;
	}

	/**
	 * Sets {@code $defs}.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public JsonSchema setDefs(Map<String, JsonSchema> value) {
		defs = value;
		return this;
	}
}
