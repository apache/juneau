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
 * Parameters for {@value org.apache.juneau.bean.mcp.McpMethods#PROMPTS_GET}.
 */
@Bean
public class GetPromptRequest {

	private String name;
	private Map<String, Object> arguments;

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
	public GetPromptRequest setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Argument values keyed by argument name.
	 *
	 * @return The arguments map, or {@code null} if not set.
	 */
	public Map<String, Object> getArguments() {
		return arguments;
	}

	/**
	 * Sets argument values.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public GetPromptRequest setArguments(Map<String, Object> value) {
		arguments = value;
		return this;
	}
}
