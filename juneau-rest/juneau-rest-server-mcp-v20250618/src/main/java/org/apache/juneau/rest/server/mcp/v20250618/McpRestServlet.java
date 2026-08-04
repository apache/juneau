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
package org.apache.juneau.rest.server.mcp.v20250618;

import org.apache.juneau.bean.mcp.v20250618.*;

/**
 * Abstract MCP servlet bound to protocol revision {@code 2025-06-18}.
 *
 * <p>
 * Subclass this (rather than {@link McpRestServlet}) to expose a {@code 2025-06-18} endpoint; the
 * revision binding is a compile-time choice made by which class you extend.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/mcp")
 * public class MyMcpServlet extends McpRestServlet {
 *     @Override
 *     protected McpServerConfig createMcpConfig() {
 *         return new McpServerConfig().setName("my-server").setVersion("1.0.0").addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110" // Inherent to extending the RestServlet hierarchy.
})
public abstract class McpRestServlet extends org.apache.juneau.rest.server.mcp.AbstractMcpRestServlet {
	private static final long serialVersionUID = 1L;

	@Override /* AbstractMcpRestServlet */
	protected McpRevision revision() {
		return new McpRevision(capabilities());
	}

	/**
	 * Optional explicit capabilities advertisement for {@code initialize}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists. Returning a non-<jk>null</jk> value bypasses auto-derivation
	 * entirely and advertises exactly what is returned — the way to advertise
	 * {@code resources.subscribe}, {@code logging}, {@code listChanged}, or {@code experimental},
	 * none of which are derivable from a handler registry.
	 *
	 * <p>
	 * This hook is typed against this revision's wire beans on purpose: capabilities diverge between
	 * MCP revisions, so a neutral carrier in the core would accept data that is nonsense for the
	 * bound revision and fail only at serialization time.
	 *
	 * <p>
	 * Stays {@code protected} (Correction C8): {@link #revision()} above is the only caller, and it
	 * lives in this same class, so there is never a need to reach this hook from outside this
	 * class's own hierarchy — in particular, {@code McpRevision} never calls back into it.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	protected ServerCapabilities capabilities() {
		return null;
	}
}
