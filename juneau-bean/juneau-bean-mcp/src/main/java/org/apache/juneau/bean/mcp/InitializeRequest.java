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
 * MCP {@code initialize} request parameters.
 */
@Bean
public class InitializeRequest {

	private String protocolVersion;
	private ClientCapabilities capabilities;
	private Implementation clientInfo;

	/**
	 * Client protocol revision.
	 *
	 * @return The protocol version, or {@code null} if not set.
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * Sets the client protocol revision.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeRequest setProtocolVersion(String value) {
		protocolVersion = value;
		return this;
	}

	/**
	 * Client capabilities.
	 *
	 * @return The capabilities, or {@code null} if not set.
	 */
	public ClientCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets client capabilities.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeRequest setCapabilities(ClientCapabilities value) {
		capabilities = value;
		return this;
	}

	/**
	 * Client implementation metadata.
	 *
	 * @return The client info, or {@code null} if not set.
	 */
	public Implementation getClientInfo() {
		return clientInfo;
	}

	/**
	 * Sets client implementation metadata.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public InitializeRequest setClientInfo(Implementation value) {
		clientInfo = value;
		return this;
	}
}
