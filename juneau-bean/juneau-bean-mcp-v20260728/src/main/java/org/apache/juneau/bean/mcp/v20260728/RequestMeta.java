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
 * Opaque per-request MCP metadata carried in the JSON-RPC {@code _meta} envelope property.
 *
 * <p>
 * Every v2 request is independently negotiated from its own {@code _meta}; no handshake or session state exists.
 */
@Marshalled
public class RequestMeta {

	private String protocolVersion;
	private Implementation clientInfo;
	private ClientCapabilities capabilities;

	/**
	 * Protocol version requested by the client.
	 *
	 * @return The protocol version, or {@code null} if not set.
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * Sets the protocol version.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public RequestMeta setProtocolVersion(String value) {
		protocolVersion = value;
		return this;
	}

	/**
	 * Client implementation identity.
	 *
	 * @return The client info, or {@code null} if not set.
	 */
	public Implementation getClientInfo() {
		return clientInfo;
	}

	/**
	 * Sets the client implementation identity.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public RequestMeta setClientInfo(Implementation value) {
		clientInfo = value;
		return this;
	}

	/**
	 * Client capability advertisement.
	 *
	 * @return The capabilities, or {@code null} if not set.
	 */
	public ClientCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets the client capability advertisement.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public RequestMeta setCapabilities(ClientCapabilities value) {
		capabilities = value;
		return this;
	}
}
