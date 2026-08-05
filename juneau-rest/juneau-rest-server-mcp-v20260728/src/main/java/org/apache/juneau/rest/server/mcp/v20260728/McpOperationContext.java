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
package org.apache.juneau.rest.server.mcp.v20260728;

import java.util.*;

/**
 * The resolved MCP operation an {@link McpOperationScopeResolver} decides required scopes for (SEP-2350 per-operation
 * step-up).
 *
 * <p>
 * Carries the JSON-RPC {@code method} (e.g. {@code tools/call}), the operation {@code name} (the tool / prompt name or
 * resource URI, as returned by the revision's routing-name derivation), and an opaque read-only view of the raw JSON-RPC
 * request {@code params}.  A static per-operation scope map keys on {@code (method, name)}; the {@code params} view is the
 * seam a future dynamic resolver could consult (READY-312f Q-c: not built for 10.0).
 *
 * <p>
 * {@code params} is the <em>raw</em> JSON-RPC {@code params} object, not the unwrapped tool-call arguments: for a
 * {@code tools/call} request it is {@code {"name":<tool>, "arguments":{...}}}, so a resolver reads the tool arguments via
 * {@code ctx.params().get("arguments")} rather than treating {@code params} as the argument map itself.
 *
 * @param method The JSON-RPC method.  Never {@code null}.
 * @param name The operation name (tool/prompt name or resource URI), or {@code null} for methods without one.
 * @param params A read-only view of the raw JSON-RPC request params.  Never {@code null}.
 * @since 10.0.0
 */
public record McpOperationContext(String method, String name, Map<String,Object> params) {

	/**
	 * Compact constructor enforcing a non-null method and defensively copying the params to a read-only map.
	 */
	public McpOperationContext {
		Objects.requireNonNull(method, "method");
		params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
	}
}
