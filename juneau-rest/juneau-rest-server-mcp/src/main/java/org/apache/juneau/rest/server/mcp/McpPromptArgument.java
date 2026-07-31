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
 * Revision-neutral declared argument of a prompt.
 *
 * <p>
 * Supersedes the wire-level {@code PromptArgument} bean.
 */
public class McpPromptArgument {

	private String name;
	private String description;
	private Boolean required;
	private McpCompleter completer;

	/**
	 * The argument name.
	 *
	 * @return The name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the argument name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptArgument setName(String value) {
		name = value;
		return this;
	}

	/**
	 * The human-readable argument description.
	 *
	 * @return The description, or <jk>null</jk> if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the argument description.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptArgument setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Whether the argument is required.
	 *
	 * @return The flag, or <jk>null</jk> if not set.
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Sets whether the argument is required.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptArgument setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * The completer invoked for a {@code completion/complete} request targeting this argument.
	 *
	 * <p>
	 * This is server behavior only: it is never mapped into a dated {@code PromptArgument} wire bean. See
	 * {@link McpServerConfig#promptCompleter(String, String)} for the neutral lookup path that resolves this
	 * property by exact prompt/argument name.
	 *
	 * @return The completer, or <jk>null</jk> if not set.
	 */
	public McpCompleter getCompleter() {
		return completer;
	}

	/**
	 * Sets the completer invoked for a {@code completion/complete} request targeting this argument.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptArgument setCompleter(McpCompleter value) {
		completer = value;
		return this;
	}
}
