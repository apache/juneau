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

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;

/**
 * A minimal Spring Boot application that serves {@link SpringExampleMcpServer} on embedded Tomcat.
 *
 * <p>
 * Run {@link #main(String[]) main} and the MCP endpoint is available at {@code http://localhost:8080/mcp}
 * (default Spring Boot port). Point {@link org.apache.juneau.examples.mcp.ExampleClient ExampleClient} at
 * that URL, or drive it with any MCP client. The three {@code @Bean} methods are the entire integration:
 *
 * <ul>
 * 	<li>the {@link GreetingService} the MCP tool resolves via Spring DI;
 * 	<li>the {@link SpringExampleMcpServer} servlet itself; and
 * 	<li>a {@link ServletRegistrationBean} mounting it at {@code /*} (so the MCP operation lands at {@code /mcp}).
 * </ul>
 */
@SpringBootApplication
public class SpringExampleApplication {

	/**
	 * The Spring-managed service injected into the MCP {@code greet} tool.
	 *
	 * @return A new {@link GreetingService}.
	 */
	@Bean
	public GreetingService greetingService() {
		return new GreetingService();
	}

	/**
	 * The MCP servlet bean (constructed by Spring so it participates in the application context).
	 *
	 * @return A new {@link SpringExampleMcpServer}.
	 */
	@Bean
	public SpringExampleMcpServer springExampleMcpServer() {
		return new SpringExampleMcpServer();
	}

	/**
	 * Mounts the MCP servlet on the embedded container.
	 *
	 * @param servlet The MCP servlet bean.
	 * @return The registration mapping the servlet at {@code /*}.
	 */
	@Bean
	public ServletRegistrationBean<SpringExampleMcpServer> mcpServletRegistration(SpringExampleMcpServer servlet) {
		return new ServletRegistrationBean<>(servlet, "/*");
	}

	/**
	 * Boots the application.
	 *
	 * @param args Standard Spring Boot arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringExampleApplication.class, args);
	}
}
