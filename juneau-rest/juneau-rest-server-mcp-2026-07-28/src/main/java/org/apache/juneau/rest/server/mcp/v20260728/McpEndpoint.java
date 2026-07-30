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

/**
 * Mixin interface that exposes a {@code 2026-07-28} MCP endpoint at {@code POST /mcp} on any Juneau REST
 * resource.
 *
 * <p>
 * The mixin path and the servlet-subclass path ({@link McpRestServlet}) are at parity: both bind the
 * revision at compile time and both expose the same {@link #capabilities()} hook.
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
public interface McpEndpoint extends org.apache.juneau.rest.server.mcp.McpEndpoint {

	@Override /* McpEndpoint */
	default org.apache.juneau.rest.server.mcp.McpRevision revision() {
		return new McpRevision(capabilities());
	}

	/**
	 * Optional explicit capabilities advertisement for {@code server/discover}.
	 *
	 * <p>
	 * Returning <jk>null</jk> (the default) leaves capabilities auto-derived from the registered
	 * tool / prompt / resource lists; a non-<jk>null</jk> value bypasses auto-derivation.
	 *
	 * @return The explicit capabilities, or <jk>null</jk> to auto-derive.
	 */
	default ServerCapabilities capabilities() {
		return null;
	}
}
