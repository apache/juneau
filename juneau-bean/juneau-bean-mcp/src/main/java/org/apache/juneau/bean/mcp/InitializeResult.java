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
 * MCP {@code initialize} result payload.
 */
@Bean
public class InitializeResult {

	private String protocolVersion;
	private ServerCapabilities capabilities;
	private Implementation serverInfo;
	private String instructions;

	/**
	 * Negotiated protocol revision.
	 *
	 * @return The protocol version, or {@code null} if not set.
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * Sets the negotiated protocol revision.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeResult setProtocolVersion(String value) {
		protocolVersion = value;
		return this;
	}

	/**
	 * Server capabilities.
	 *
	 * @return The capabilities, or {@code null} if not set.
	 */
	public ServerCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets server capabilities.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeResult setCapabilities(ServerCapabilities value) {
		capabilities = value;
		return this;
	}

	/**
	 * Server implementation metadata.
	 *
	 * @return The server info, or {@code null} if not set.
	 */
	public Implementation getServerInfo() {
		return serverInfo;
	}

	/**
	 * Sets server implementation metadata.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeResult setServerInfo(Implementation value) {
		serverInfo = value;
		return this;
	}

	/**
	 * Optional instructions for the client.
	 *
	 * @return The instructions, or {@code null} if not set.
	 */
	public String getInstructions() {
		return instructions;
	}

	/**
	 * Sets optional instructions.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeResult setInstructions(String value) {
		instructions = value;
		return this;
	}
}
