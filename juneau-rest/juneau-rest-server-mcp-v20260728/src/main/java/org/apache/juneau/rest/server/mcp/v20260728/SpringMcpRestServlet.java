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

import org.apache.juneau.rest.server.springboot.*;

/**
 * Convenience base class exposing a {@code 2026-07-28} MCP endpoint on a Spring Boot resource with full
 * Spring dependency-injection support.
 *
 * <p>
 * Combines {@link BasicSpringRestServlet} (Spring Boot bean-store integration via
 * {@link org.apache.juneau.rest.server.springboot.SpringBeanStore}) with the {@link McpEndpoint} mixin
 * (the {@code 2026-07-28} MCP handler). This is the recommended way to expose MCP from a Spring Boot
 * resource: subclassing {@link McpRestServlet} instead does <b>not</b> work, since
 * {@link McpRestServlet} and {@link BasicSpringRestServlet} are sibling
 * {@code RestServlet} subclasses and a class cannot extend both — and even if it could, only the mixin
 * path preserves the Spring {@code BeanStore} bridge that MCP tool/prompt/resource handlers rely on to
 * resolve Spring-managed beans (for example via {@code BeanStore.getBean(MyService.class)}).
 *
 * <p>
 * Subclasses supply their {@link org.apache.juneau.rest.server.mcp.McpServerConfig} by implementing
 * {@link #getMcpConfig()}, and may optionally override {@link McpEndpoint#getMcpOptions()} to customize
 * capabilities/instructions/cache/mrtr/subscriptions/broker behavior.
 *
 * <h5 class='section'>Example:</h5>
 * <pre>
 * @Rest(path="/mcp")
 * public class MyMcpResource extends SpringMcpRestServlet {
 * 	@Override
 * 	public McpServerConfig getMcpConfig() {
 * 		return new McpServerConfig().addTool(new MySpringAwareTool());
 * 	}
 * 	@Override                                        // optional
 * 	public McpOptions getMcpOptions() {
 * 		return new McpOptions().mrtr(m -&gt; m.setTtlMs(600_000));
 * 	}
 * }
 * </pre>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S2176", // Intentional: dated adapter binding classes are de-versioned and differentiated by package.
	"java:S110" // Inheritance depth is inherent to the Juneau Spring REST servlet hierarchy.
})
public abstract class SpringMcpRestServlet extends BasicSpringRestServlet implements McpEndpoint {
	private static final long serialVersionUID = 1L;
}
