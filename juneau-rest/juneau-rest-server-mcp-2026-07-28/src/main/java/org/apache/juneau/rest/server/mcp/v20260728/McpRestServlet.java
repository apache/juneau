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

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.server.*;

/**
 * Abstract MCP servlet bound to protocol revision {@code 2026-07-28}.
 *
 * <p>
 * Subclass this (rather than the neutral {@link org.apache.juneau.rest.server.mcp.McpRestServlet}) to
 * expose a {@code 2026-07-28} endpoint at {@code POST /}; the revision binding is a compile-time choice
 * made by which class you extend.
 *
 * <p>
 * URI and polymorphic-type serializer policy ({@code addBeanTypes} and {@code uriResolution="NONE"}) is
 * inherited centrally from the neutral {@link org.apache.juneau.rest.server.mcp.McpRestServlet}; the only
 * revision-specific behavior added here is notification empty-body processing, below.
 *
 * <p>
 * Registers {@link McpNotificationResponseProcessor} so a {@code null} dispatch result (a JSON-RPC
 * notification) renders as a genuinely empty HTTP body instead of the framework's default four-byte
 * {@code null} JSON literal.
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
@Rest(responseProcessors = McpNotificationResponseProcessor.class)
public abstract class McpRestServlet extends org.apache.juneau.rest.server.mcp.McpRestServlet {
	private static final long serialVersionUID = 1L;

	@Override /* McpRestServlet */
	protected org.apache.juneau.rest.server.mcp.McpRevision revision() {
		return new McpRevision(capabilities());
	}

	/**
	 * Optional explicit capabilities advertisement for {@code server/discover}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists. Returning a non-<jk>null</jk> value bypasses auto-derivation
	 * entirely and advertises exactly what is returned — the way to advertise capabilities that are
	 * not derivable from a handler registry.
	 *
	 * <p>
	 * This hook is typed against this revision's wire beans on purpose: capabilities diverge between
	 * MCP revisions, so a neutral carrier in the core would accept data that is nonsense for the
	 * bound revision and fail only at serialization time.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	protected ServerCapabilities capabilities() {
		return null;
	}
}
