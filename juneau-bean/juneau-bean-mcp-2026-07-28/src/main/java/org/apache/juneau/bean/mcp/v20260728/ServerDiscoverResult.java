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
 * Result payload for {@value McpMethods#SERVER_DISCOVER}.
 *
 * <p>
 * Carries only server identity and capabilities; there is no protocol version, session, or handshake state.
 */
@Marshalled
public class ServerDiscoverResult {

	private Implementation serverInfo;
	private ServerCapabilities capabilities;

	/**
	 * Server implementation identity.
	 *
	 * @return The server info, or {@code null} if not set.
	 */
	public Implementation getServerInfo() {
		return serverInfo;
	}

	/**
	 * Sets the server implementation identity.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult setServerInfo(Implementation value) {
		serverInfo = value;
		return this;
	}

	/**
	 * Server capability advertisement.
	 *
	 * @return The capabilities, or {@code null} if not set.
	 */
	public ServerCapabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Sets the server capability advertisement.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult setCapabilities(ServerCapabilities value) {
		capabilities = value;
		return this;
	}
}
