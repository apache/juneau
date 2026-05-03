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
 * Single message line inside {@link GetPromptResult}.
 */
@Bean
public class PromptMessage {

	private Role role;
	private Content content;

	/**
	 * Message role.
	 *
	 * @return The role, or {@code null} if not set.
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Sets the message role.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public PromptMessage setRole(Role value) {
		role = value;
		return this;
	}

	/**
	 * Message content blocks.
	 *
	 * @return The content, or {@code null} if not set.
	 */
	public Content getContent() {
		return content;
	}

	/**
	 * Sets the message content.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public PromptMessage setContent(Content value) {
		content = value;
		return this;
	}
}
