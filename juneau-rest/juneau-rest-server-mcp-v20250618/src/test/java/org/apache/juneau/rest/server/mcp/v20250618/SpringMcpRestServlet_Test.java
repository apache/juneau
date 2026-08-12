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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link SpringMcpRestServlet}.
 *
 * <p>
 * A plain-construction check that a concrete subclass instantiates cleanly and its
 * {@link McpEndpoint#getMcpConfig() getMcpConfig()} override is reachable -- mirroring
 * {@code McpRestServlet_Test#c01_servlet_getMcpConfig_cachesValue}'s style, but for the Spring Boot
 * base-class flavor. No Spring context is required: {@link org.apache.juneau.rest.server.springboot.BasicSpringRestServlet
 * BasicSpringRestServlet} (like other {@code RestServlet} subclasses) does no Spring-specific work in
 * its constructor -- only at servlet {@code init()} time -- so this is a genuine plain-object unit test,
 * not a Spring-container integration test.
 */
class SpringMcpRestServlet_Test {

	public static class A extends SpringMcpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().setName("spring-test").setVersion("1.0.0");
		}
	}

	@Test void a01_concreteSubclass_constructsAndExposesMcpConfig() {
		var s = new A();
		var cfg = s.getMcpConfig();
		assertEquals("spring-test", cfg.getName());
	}
}
