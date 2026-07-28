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

import java.util.*;

/**
 * Revision-neutral result of a {@code tools/call} invocation.
 *
 * <p>
 * Supersedes the wire-level {@code CallToolResult} bean. Deliberately absent:
 * {@code structuredContent} and {@code cachePolicy}, both specific to later MCP revisions.
 *
 * <p>
 * Note the field is named {@code error}, while the {@code 2025-06-18} wire property is
 * {@code isError}; the rename happens at the adapter boundary and is intentional. Do not
 * "fix" this field name to match the wire.
 */
public class McpToolOutcome {

	private List<McpContentBlock> content;
	private Boolean error;

	/**
	 * Creates an outcome carrying a single text block.
	 *
	 * @param value The text. Can be <jk>null</jk>.
	 * @return A new outcome. Never <jk>null</jk>.
	 */
	public static McpToolOutcome text(String value) {
		return new McpToolOutcome().setContent(List.of(McpContentBlock.text(value)));
	}

	/**
	 * Creates an outcome carrying the supplied blocks.
	 *
	 * @param values The content blocks. Must not be <jk>null</jk>.
	 * @return A new outcome. Never <jk>null</jk>.
	 */
	public static McpToolOutcome of(McpContentBlock... values) {
		return new McpToolOutcome().setContent(List.of(values));
	}

	/**
	 * The content blocks returned by the tool.
	 *
	 * @return The blocks, or <jk>null</jk> if not set. A <jk>null</jk> value is distinct from an empty
	 * 	list: it means the wire property is omitted entirely.
	 */
	public List<McpContentBlock> getContent() {
		return content;
	}

	/**
	 * Sets the content blocks.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolOutcome setContent(List<McpContentBlock> value) {
		content = value;
		return this;
	}

	/**
	 * Whether the tool call represents an error result.
	 *
	 * @return The flag, or <jk>null</jk> if not set.
	 */
	public Boolean getError() {
		return error;
	}

	/**
	 * Sets the error flag.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpToolOutcome setError(Boolean value) {
		error = value;
		return this;
	}
}
