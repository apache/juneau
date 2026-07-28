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
 * Revision-neutral single message within a prompt result.
 *
 * <p>
 * Supersedes the wire-level {@code PromptMessage} bean.
 */
public class McpPromptMessage {

	private McpRole role;
	private McpContentBlock content;

	/**
	 * The message's role.
	 *
	 * @return The role, or <jk>null</jk> if not set.
	 */
	public McpRole getRole() {
		return role;
	}

	/**
	 * Sets the message's role.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptMessage setRole(McpRole value) {
		role = value;
		return this;
	}

	/**
	 * The message's content.
	 *
	 * @return The content, or <jk>null</jk> if not set.
	 */
	public McpContentBlock getContent() {
		return content;
	}

	/**
	 * Sets the message's content.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpPromptMessage setContent(McpContentBlock value) {
		content = value;
		return this;
	}
}
