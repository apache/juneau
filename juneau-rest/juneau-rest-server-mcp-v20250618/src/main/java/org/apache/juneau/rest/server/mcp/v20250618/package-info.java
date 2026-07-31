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

/**
 * MCP {@code 2025-06-18} binding for {@code juneau-rest-server-mcp}.
 *
 * <p>
 * Contains everything that knows about the {@code 2025-06-18} wire format: the JSON-RPC method
 * table, the mapping between the revision-neutral model in
 * {@link org.apache.juneau.rest.server.mcp} and the wire beans in
 * {@link org.apache.juneau.bean.mcp.v20250618}, this revision's JSON-RPC error-code table, and the
 * two entry points a consumer binds against
 * ({@link org.apache.juneau.rest.server.mcp.v20250618.McpRestServlet} for the
 * servlet-subclass path and
 * {@link org.apache.juneau.rest.server.mcp.v20250618.McpEndpoint} for the mixin path).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerMcp">juneau-rest-server-mcp</a>
 * </ul>
 */
package org.apache.juneau.rest.server.mcp.v20250618;
