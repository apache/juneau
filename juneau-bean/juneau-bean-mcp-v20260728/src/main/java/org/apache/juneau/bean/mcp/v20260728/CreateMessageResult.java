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
package org.apache.juneau.bean.mcp.v20260728;

import org.apache.juneau.marshall.*;

/**
 * Result payload for {@value McpMethods#SAMPLING_CREATE_MESSAGE} (MCP sampling). Never
 * dispatched through {@code McpRevision}; see {@link CreateMessageRequest}'s class Javadoc for the same
 * plain-POJO/duplex-payload rationale.
 */
@Marshalled
public class CreateMessageResult {

	private Role role;
	private Content content;
	private String model;
	private String stopReason;

	/**
	 * The generated message's role (real schema restricts this to {@code user}/{@code assistant}).
	 *
	 * @return The role, or {@code null} if not set.
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Sets the role.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageResult setRole(Role value) {
		role = value;
		return this;
	}

	/**
	 * The generated content — a single block ({@code text}/{@code image}/{@code audio}/{@code resource}).
	 *
	 * @return The content, or {@code null} if not set.
	 */
	public Content getContent() {
		return content;
	}

	/**
	 * Sets the content.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageResult setContent(Content value) {
		content = value;
		return this;
	}

	/**
	 * The model that generated the content.
	 *
	 * @return The model name, or {@code null} if not set.
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Sets the model name.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageResult setModel(String value) {
		model = value;
		return this;
	}

	/**
	 * Why sampling stopped (commonly {@code endTurn}/{@code stopSequence}/{@code maxTokens}, but the schema
	 * also permits arbitrary server-defined strings, so this stays a plain {@code String}).
	 *
	 * @return The stop reason, or {@code null} if not set.
	 */
	public String getStopReason() {
		return stopReason;
	}

	/**
	 * Sets the stop reason.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public CreateMessageResult setStopReason(String value) {
		stopReason = value;
		return this;
	}
}
