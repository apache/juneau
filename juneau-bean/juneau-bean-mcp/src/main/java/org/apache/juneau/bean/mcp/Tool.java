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

import org.apache.juneau.annotation.*;

/**
 * MCP tool descriptor ({@code tools/list} entry).
 */
@Bean
public class Tool {

	private String name;
	private String description;
	private JsonSchema inputSchema;

	/**
	 * Tool name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the tool name.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Tool setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Human-readable description.
	 *
	 * @return The description, or {@code null} if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Tool setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * JSON Schema for tool arguments.
	 *
	 * @return The input schema, or {@code null} if not set.
	 */
	public JsonSchema getInputSchema() {
		return inputSchema;
	}

	/**
	 * Sets the input schema.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Tool setInputSchema(JsonSchema value) {
		inputSchema = value;
		return this;
	}
}
