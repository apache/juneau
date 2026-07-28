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
 * Revision-neutral core for MCP endpoints built on {@code juneau-rest-server}.
 *
 * <p>
 * This package knows the shape of an MCP server — tools, prompts, resources, pagination, the
 * JSON-RPC dispatch contract, and the two HTTP entry points — without knowing any MCP protocol
 * revision. It has no compile dependency on any revision's wire beans, and that is enforced at
 * build time. A protocol revision is supplied by an implementation of {@link McpRevision} living in
 * its own module (for example {@code juneau-rest-server-mcp-2025-06-18}), and a consumer binds one
 * at compile time by extending that revision's abstract servlet or composing its endpoint mixin.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestServerMcp">juneau-rest-server-mcp</a>
 * </ul>
 */
package org.apache.juneau.rest.server.mcp;
