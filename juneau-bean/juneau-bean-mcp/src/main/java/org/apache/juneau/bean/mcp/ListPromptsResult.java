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
 * Result payload for {@value org.apache.juneau.bean.mcp.McpMethods#PROMPTS_LIST}.
 */
@Bean
public class ListPromptsResult {

	private List<Prompt> prompts;
	private String nextCursor;

	/**
	 * Prompt descriptors.
	 *
	 * @return The prompts list, or {@code null} if not set.
	 */
	public List<Prompt> getPrompts() {
		return prompts;
	}

	/**
	 * Sets the prompt descriptors.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ListPromptsResult setPrompts(List<Prompt> value) {
		prompts = value;
		return this;
	}

	/**
	 * Pagination cursor for the next page.
	 *
	 * @return The cursor, or {@code null} if not set.
	 */
	public String getNextCursor() {
		return nextCursor;
	}

	/**
	 * Sets the next cursor.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ListPromptsResult setNextCursor(String value) {
		nextCursor = value;
		return this;
	}
}
