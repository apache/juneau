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
 * Resolves the set of scopes required to invoke a specific MCP operation (SEP-2350 per-operation step-up enforcement).
 *
 * <p>
 * The {@code 2026-07-28} resource server enforces per-operation scopes at the POST-parse dispatch point (where the
 * JSON-RPC method is known); a resolver returning a non-empty set that the caller's granted scopes do not satisfy causes
 * a {@code 403 insufficient_scope} step-up challenge naming the required scopes.
 *
 * <p>
 * The default resolver is a <b>static per-operation map</b> populated via
 * {@link McpResourceServerConfig#addOperationScope(String, String, String...)} &mdash; sufficient for 10.0 per the
 * READY-312f settled premise.  This interface is the seam a future <em>dynamic</em> resolver (e.g. one inspecting
 * {@link McpOperationContext#params() params}) could plug into via
 * {@link McpResourceServerConfig#setOperationScopeResolver(McpOperationScopeResolver)}; that dynamic capability is
 * deliberately not built for 10.0.
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface McpOperationScopeResolver {

	/**
	 * Returns the scopes required to invoke the given operation.
	 *
	 * @param ctx The resolved operation (method, name, raw JSON-RPC params).  Never {@code null}.
	 * @return The required scopes.  An empty or {@code null} return means "no per-operation requirement beyond the
	 * 	endpoint-wide baseline".
	 */
	Set<String> requiredScopesFor(McpOperationContext ctx);
}
