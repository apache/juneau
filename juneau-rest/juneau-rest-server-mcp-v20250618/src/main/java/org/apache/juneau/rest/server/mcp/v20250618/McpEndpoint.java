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
 * Mixin interface that exposes a {@code 2025-06-18} MCP endpoint at {@code POST /mcp} on any Juneau
 * REST resource.
 *
 * <p>
 * The mixin path and the servlet-subclass path
 * ({@link McpRestServlet}) are at parity: both bind the revision at compile time and both
 * expose the same {@link #capabilities()} hook.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/api")
 * public class MyResource extends BasicRestServlet implements McpEndpoint {
 *     @Override
 *     public McpServerConfig getMcpConfig() {
 *         return new McpServerConfig().addTool(new MyEchoTool());
 *     }
 * }
 * </pre>
 */
@SuppressWarnings({
	"java:S2176" // Intentional: dated adapter binding classes are de-versioned and differentiated by package (see TODO-312).
})
public interface McpEndpoint extends org.apache.juneau.rest.server.mcp.McpEndpoint {

	@Override /* McpEndpoint */
	default McpRevision revision() {
		return new McpRevision(capabilities());
	}

	/**
	 * Optional explicit capabilities advertisement for {@code initialize}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists; a non-<jk>null</jk> value bypasses auto-derivation.
	 *
	 * <p>
	 * Stays a {@code default} interface method (Correction C8), for the same reason
	 * {@link McpRestServlet#capabilities()} stays {@code protected}: only {@link #revision()}
	 * above, declared on this same interface, ever calls it.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	default ServerCapabilities capabilities() {
		return null;
	}
}
