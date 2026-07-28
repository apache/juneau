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
 * Revision-neutral prompt descriptor returned from {@code prompts/list}.
 *
 * <p>
 * Supersedes the wire-level {@code Prompt} bean.
 */
public class McpPromptSpec {

	private String name;
	private String description;
	private List<McpPromptArgument> arguments;

	/**
	 * The prompt name, used to route {@code prompts/get} requests.
	 *
	 * @return The name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the prompt name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptSpec setName(String value) {
		name = value;
		return this;
	}

	/**
	 * The human-readable prompt description.
	 *
	 * @return The description, or <jk>null</jk> if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the prompt description.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptSpec setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * The prompt's declared arguments.
	 *
	 * @return The arguments, or <jk>null</jk> if not set. A <jk>null</jk> value is distinct from an
	 * 	empty list: it means the wire property is omitted entirely.
	 */
	public List<McpPromptArgument> getArguments() {
		return arguments;
	}

	/**
	 * Sets the prompt's declared arguments.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptSpec setArguments(List<McpPromptArgument> value) {
		arguments = value;
		return this;
	}
}
