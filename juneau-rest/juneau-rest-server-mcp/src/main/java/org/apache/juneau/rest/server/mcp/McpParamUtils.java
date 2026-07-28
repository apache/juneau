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
package org.apache.juneau.rest.server.mcp;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;

/**
 * Shared internal plumbing for revision {@link McpRevision#dispatch} implementations.
 *
 * <p>
 * Untyped-parameter coercion and typed-argument extraction. Not a general-purpose public API: the
 * methods are {@code public} only because a revision adapter lives in a different package from this
 * one ({@code org.apache.juneau.rest.server.mcp.v20250618} vs. {@code org.apache.juneau.rest.server.mcp}),
 * and Java package-private visibility does not span that boundary.
 *
 * <p>
 * The {@code -32602} code these methods raise is the <b>JSON-RPC 2.0 standard</b> "Invalid params"
 * code, not a per-revision choice, which is why it is fixed here rather than routed through
 * {@link McpRevision#errorCode(McpErrorKind)}. A revision that needs a different code for malformed
 * parameters must coerce parameters itself instead of using these helpers.
 */
public final class McpParamUtils {

	private static final int CODE_INVALID_PARAMS = -32602;

	private McpParamUtils() {}

	/**
	 * Coerces a JSON-RPC {@code params} value to a string-keyed map.
	 *
	 * @param params The raw params value. Can be <jk>null</jk>.
	 * @return The params as a map; an empty map when {@code params} is <jk>null</jk>. Never <jk>null</jk>.
	 * @throws McpException If {@code params} is present but is not a JSON object.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type parameter verified by MCP protocol contract.
	})
	public static Map<String,Object> asMap(Object params) {
		if (params == null)
			return Map.of();
		if (params instanceof Map)
			return (Map<String,Object>) params;
		throw new McpException(CODE_INVALID_PARAMS, "Params must be an object");
	}

	/**
	 * Reads a parameter as a string.
	 *
	 * @param args The params map. Never <jk>null</jk>.
	 * @param key The parameter name.
	 * @return The value's {@code toString()}, or <jk>null</jk> if absent.
	 */
	public static String strParam(Map<String,Object> args, String key) {
		var v = args.get(key);
		return v == null ? null : v.toString();
	}

	/**
	 * Reads a parameter as a nested string-keyed map.
	 *
	 * @param args The params map. Never <jk>null</jk>.
	 * @param key The parameter name.
	 * @return The nested map; an empty map when the parameter is absent. Never <jk>null</jk>.
	 * @throws McpException If the parameter is present but is not a JSON object.
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type parameter verified by MCP protocol contract.
	})
	public static Map<String,Object> mapParam(Map<String,Object> args, String key) {
		var v = args.get(key);
		if (v == null)
			return Map.of();
		if (v instanceof Map)
			return (Map<String,Object>) v;
		throw new McpException(CODE_INVALID_PARAMS, "Param '" + key + "' must be an object");
	}
}
