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
package org.apache.juneau.rest.mcp;

import java.util.*;

import org.apache.juneau.bean.mcp.*;

/**
 * Aggregate configuration consumed by {@link McpDispatcher}.
 *
 * <p>
 * Applications register a single {@link McpServerConfig} (typically as a bean in their {@code RestContext}
 * bean store) listing the tools, prompts, and resources to expose, plus optional server metadata and a
 * pagination strategy.
 */
public class McpServerConfig {

	private Implementation serverInfo;
	private String protocolVersion = McpProtocol.VERSION_2025_06_18;
	private String instructions;
	private List<McpToolHandler> tools = new ArrayList<>();
	private List<McpPromptHandler> prompts = new ArrayList<>();
	private List<McpResourceHandler> resources = new ArrayList<>();
	private ServerCapabilities capabilities;
	private McpCursor cursor = McpCursor.SINGLE_PAGE;

	/**
	 * Server identity reported in {@code initialize}.
	 *
	 * <p>
	 * If {@code null}, {@link McpDispatcher} fills in {@code "juneau-rest-server-mcp" / "unknown"}.
	 *
	 * @return The server info, or {@code null} if not set.
	 */
	public Implementation getServerInfo() {
		return serverInfo;
	}

	/**
	 * Sets the server identity.
	 *
	 * @param serverInfo The new value.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setServerInfo(Implementation serverInfo) {
		this.serverInfo = serverInfo;
		return this;
	}

	/**
	 * MCP protocol revision returned by {@code initialize}.
	 *
	 * @return The protocol version.
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * Sets the protocol revision.
	 *
	 * @param protocolVersion The new value.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
		return this;
	}

	/**
	 * Optional instructions surfaced via {@code initialize}.
	 *
	 * @return The instructions, or {@code null} if not set.
	 */
	public String getInstructions() {
		return instructions;
	}

	/**
	 * Sets initialize instructions.
	 *
	 * @param instructions The new value.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setInstructions(String instructions) {
		this.instructions = instructions;
		return this;
	}

	/**
	 * Registered tool handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpToolHandler> getTools() {
		return tools;
	}

	/**
	 * Sets the tool handler list.
	 *
	 * @param tools The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setTools(List<McpToolHandler> tools) {
		this.tools = tools == null ? new ArrayList<>() : new ArrayList<>(tools);
		return this;
	}

	/**
	 * Convenience: append one or more tool handlers.
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addTool(McpToolHandler... handlers) {
		Collections.addAll(this.tools, handlers);
		return this;
	}

	/**
	 * Registered prompt handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpPromptHandler> getPrompts() {
		return prompts;
	}

	/**
	 * Sets the prompt handler list.
	 *
	 * @param prompts The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setPrompts(List<McpPromptHandler> prompts) {
		this.prompts = prompts == null ? new ArrayList<>() : new ArrayList<>(prompts);
		return this;
	}

	/**
	 * Convenience: append one or more prompt handlers.
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addPrompt(McpPromptHandler... handlers) {
		Collections.addAll(this.prompts, handlers);
		return this;
	}

	/**
	 * Registered resource handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpResourceHandler> getResources() {
		return resources;
	}

	/**
	 * Sets the resource handler list.
	 *
	 * @param resources The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setResources(List<McpResourceHandler> resources) {
		this.resources = resources == null ? new ArrayList<>() : new ArrayList<>(resources);
		return this;
	}

	/**
	 * Convenience: append one or more resource handlers.
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addResource(McpResourceHandler... handlers) {
		Collections.addAll(this.resources, handlers);
		return this;
	}

	/**
	 * Optional explicit capabilities advertisement.
	 *
	 * <p>
	 * When {@code null}, {@link McpDispatcher} synthesizes one from the registered handler lists.
	 *
	 * @return The override, or {@code null} if auto-derived.
	 */
	public ServerCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets the explicit capabilities advertisement.
	 *
	 * @param capabilities The new value.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setCapabilities(ServerCapabilities capabilities) {
		this.capabilities = capabilities;
		return this;
	}

	/**
	 * Pagination strategy for {@code list} dispatchers (tools / prompts / resources).
	 *
	 * @return The cursor. Never {@code null} (defaults to {@link McpCursor#SINGLE_PAGE}).
	 */
	public McpCursor getCursor() {
		return cursor;
	}

	/**
	 * Sets the pagination strategy.
	 *
	 * @param cursor The new value (or {@code null} to reset to {@link McpCursor#SINGLE_PAGE}).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setCursor(McpCursor cursor) {
		this.cursor = cursor == null ? McpCursor.SINGLE_PAGE : cursor;
		return this;
	}
}
