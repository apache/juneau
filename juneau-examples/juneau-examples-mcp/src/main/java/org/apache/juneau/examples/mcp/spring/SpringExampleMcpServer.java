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
package org.apache.juneau.examples.mcp.spring;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.mcp.v20260728.*;

/**
 * The Spring Boot flavor of the MCP server.
 *
 * <p>
 * Extending {@link SpringMcpRestServlet} (rather than the plain {@code McpRestServlet}) is the whole point:
 * it bridges the Spring {@code ApplicationContext} into each MCP handler's per-request {@link org.apache.juneau.commons.inject.BeanStore},
 * so {@code ctx.getBean(GreetingService.class)} resolves the real Spring-managed {@link GreetingService}
 * singleton. The same handler on a plain {@code McpRestServlet} would not see Spring beans.
 *
 * <p>
 * When registered at servlet mapping {@code /*} (see {@link SpringExampleApplication}), the MCP endpoint is
 * served at {@code /mcp} (the Spring convenience servlet mounts its operation at {@code /mcp} relative to the
 * servlet root).
 *
 * @serial exclude
 */
@Rest(serializers = {JsonSerializer.class, SseSerializer.class}, parsers = JsonParser.class, defaultAccept = "application/json")
public class SpringExampleMcpServer extends SpringMcpRestServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public McpServerConfig getMcpConfig() {
		return new McpServerConfig()
			.setName("juneau-spring-greeting-example")
			.setVersion("1.0.0")
			.addTool(McpToolHandler.of(
				new McpToolSpec().setName("greet").setDescription("Greets a name using a Spring-injected GreetingService."),
				(arguments, ctx) -> {
					var name = String.valueOf(arguments.getOrDefault("name", "world"));
					return McpToolOutcome.text(ctx.getBean(GreetingService.class)
						.map(service -> service.greet(name))
						.orElse("(GreetingService was not resolvable from the BeanStore)"));
				}));
	}

	/**
	 * On revision {@code 2026-07-28}, {@code server/discover}'s {@code instructions} field is sourced from
	 * {@link McpOptions}, <b>not</b> {@link McpServerConfig#setInstructions}, which only feeds the legacy v1
	 * {@code initialize} handshake.
	 */
	@Override
	public McpOptions getMcpOptions() {
		return new McpOptions()
			.setInstructions("Call the 'greet' tool; its greeting is produced by a Spring-managed bean.");
	}
}
