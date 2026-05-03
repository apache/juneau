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
 * Result payload for {@value org.apache.juneau.bean.mcp.McpMethods#PROMPTS_GET}.
 */
@Bean
public class GetPromptResult {

	private String description;
	private List<PromptMessage> messages;

	/**
	 * Prompt description.
	 *
	 * @return The description, or {@code null} if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the prompt description.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public GetPromptResult setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Rendered prompt messages.
	 *
	 * @return The messages list, or {@code null} if not set.
	 */
	public List<PromptMessage> getMessages() {
		return messages;
	}

	/**
	 * Sets rendered messages.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public GetPromptResult setMessages(List<PromptMessage> value) {
		messages = value;
		return this;
	}
}
