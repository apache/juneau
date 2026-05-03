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
 * MCP prompt descriptor ({@code prompts/list} entry).
 */
@Bean
public class Prompt {

	private String name;
	private String description;
	private List<PromptArgument> arguments;

	/**
	 * Prompt name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the prompt name.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Prompt setName(String value) {
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
	public Prompt setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Declared prompt arguments.
	 *
	 * @return The arguments list, or {@code null} if not set.
	 */
	public List<PromptArgument> getArguments() {
		return arguments;
	}

	/**
	 * Sets declared arguments.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Prompt setArguments(List<PromptArgument> value) {
		arguments = value;
		return this;
	}
}
