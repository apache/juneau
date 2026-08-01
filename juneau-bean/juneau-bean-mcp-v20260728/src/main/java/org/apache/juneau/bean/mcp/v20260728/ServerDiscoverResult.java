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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * Result payload for {@value McpMethods#SERVER_DISCOVER}.
 *
 * <p>
 * Extends {@link CacheableResult} so discovery participates in the same SEP-2549 cache-hint machinery as the
 * list/read results, and (transitively) {@link Result} so it inherits the required {@code resultType}
 * discriminator plus {@code _meta}. Server identity belongs on the inherited {@code _meta} per the schema; there
 * is no protocol version, session, or handshake state, and no top-level {@code serverInfo} member. Use the
 * inherited {@link Result#getMeta()} server identity instead.
 */
@Marshalled
public class ServerDiscoverResult extends CacheableResult<ServerDiscoverResult> {

	private List<String> supportedVersions;
	private ServerCapabilities capabilities;
	private String instructions;

	/**
	 * Protocol versions this server supports, in preference order.
	 *
	 * @return The supported-versions list, or {@code null} if not set.
	 */
	public List<String> getSupportedVersions() {
		return u(supportedVersions);
	}

	/**
	 * Sets the supported protocol versions.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult setSupportedVersions(List<String> value) {
		supportedVersions = value;
		return this;
	}

	/**
	 * Sets the supported protocol versions.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult setSupportedVersions(String...value) {
		supportedVersions = list(value);
		return this;
	}

	/**
	 * Appends to the supported protocol versions.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult addSupportedVersions(String...value) {
		if (supportedVersions == null)
			supportedVersions = list();
		Collections.addAll(supportedVersions, value);
		return this;
	}

	/**
	 * Appends to the supported protocol versions.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult addSupportedVersions(Collection<String> value) {
		if (supportedVersions == null)
			supportedVersions = list();
		supportedVersions.addAll(value);
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

	/**
	 * Optional free-form usage instructions for the client.
	 *
	 * @return The instructions, or {@code null} if not set.
	 */
	public String getInstructions() {
		return instructions;
	}

	/**
	 * Sets the usage instructions.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ServerDiscoverResult setInstructions(String value) {
		instructions = value;
		return this;
	}
}
