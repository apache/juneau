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
 * MCP server capability advertisement in {@link InitializeResult}.
 */
@Bean
public class ServerCapabilities {

	private ToolCapability tools;
	private PromptCapability prompts;
	private ResourceCapability resources;
	private LoggingCapability logging;
	private Map<String, Object> experimental;

	/**
	 * Tools capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public ToolCapability getTools() {
		return tools;
	}

	/**
	 * Sets tools capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ServerCapabilities setTools(ToolCapability value) {
		tools = value;
		return this;
	}

	/**
	 * Prompts capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public PromptCapability getPrompts() {
		return prompts;
	}

	/**
	 * Sets prompts capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ServerCapabilities setPrompts(PromptCapability value) {
		prompts = value;
		return this;
	}

	/**
	 * Resources capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public ResourceCapability getResources() {
		return resources;
	}

	/**
	 * Sets resources capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ServerCapabilities setResources(ResourceCapability value) {
		resources = value;
		return this;
	}

	/**
	 * Logging capability.
	 *
	 * @return The capability, or {@code null} if not set.
	 */
	public LoggingCapability getLogging() {
		return logging;
	}

	/**
	 * Sets logging capability.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ServerCapabilities setLogging(LoggingCapability value) {
		logging = value;
		return this;
	}

	/**
	 * Experimental capability extensions.
	 *
	 * @return The experimental map, or {@code null} if not set.
	 */
	public Map<String, Object> getExperimental() {
		return experimental;
	}

	/**
	 * Sets experimental extensions.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public ServerCapabilities setExperimental(Map<String, Object> value) {
		experimental = value;
		return this;
	}
}
