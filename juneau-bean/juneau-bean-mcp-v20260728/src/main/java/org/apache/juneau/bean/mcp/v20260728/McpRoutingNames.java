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

import java.util.*;

/**
 * Shared SEP-2243 routing-name helper used by both the {@code 2026-07-28} server adapter and client adapter.
 *
 * @since 10.0.0
 */
public final class McpRoutingNames {

	private McpRoutingNames() {}

	/**
	 * Returns the routing name for {@code Mcp-Name} agreement checks and client header stamping.
	 *
	 * @param method The JSON-RPC method name.
	 * @param params The request params object.
	 * @return The routing name, or empty string.
	 */
	public static String routingName(String method, Object params) {
		var name = switch (method) {
			case McpMethods.TOOLS_CALL, McpMethods.PROMPTS_GET -> paramValue(params, "name");
			case McpMethods.RESOURCES_READ -> paramValue(params, "uri");
			default -> "";
		};
		return name == null ? "" : name;
	}

	private static String paramValue(Object params, String key) {
		if (params instanceof Map<?,?> m) {
			var v = m.get(key);
			return v == null ? null : v.toString();
		}
		return null;
	}
}
