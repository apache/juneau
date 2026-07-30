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
package org.apache.juneau.rest.server.mcp;

/**
 * Revision-neutral tool descriptor returned from {@code tools/list}.
 *
 * <p>
 * Supersedes the wire-level {@code Tool} bean. {@code outputSchema} is revision-neutral: structured
 * tool output was introduced in {@code 2025-06-18}, and adapters narrow its schema shape for earlier
 * revisions. Deliberately absent: {@code title}, which is specific to later MCP revisions and belongs
 * on the revision-typed side of the adapter boundary.
 */
public class McpToolSpec {

	private String name;
	private String description;
	private McpSchema inputSchema;
	private McpSchema outputSchema;

	/**
	 * The tool name, used to route {@code tools/call} requests.
	 *
	 * @return The name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the tool name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolSpec setName(String value) {
		name = value;
		return this;
	}

	/**
	 * The human-readable tool description.
	 *
	 * @return The description, or <jk>null</jk> if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the tool description.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolSpec setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * The tool's argument schema.
	 *
	 * @return The schema, or <jk>null</jk> if not set.
	 */
	public McpSchema getInputSchema() {
		return inputSchema;
	}

	/**
	 * Sets the tool's argument schema.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolSpec setInputSchema(McpSchema value) {
		inputSchema = value;
		return this;
	}

	/**
	 * The tool's structured result schema.
	 *
	 * @return The schema, or <jk>null</jk> if not set.
	 */
	public McpSchema getOutputSchema() {
		return outputSchema;
	}

	/**
	 * Sets the tool's structured result schema.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolSpec setOutputSchema(McpSchema value) {
		outputSchema = value;
		return this;
	}
}
