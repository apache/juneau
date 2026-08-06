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

/**
 * An ordinary Spring-managed service, injected into an MCP tool handler by {@link SpringExampleMcpServer}
 * to prove that Spring dependency injection flows into MCP handlers when the server extends
 * {@code SpringMcpRestServlet}.
 */
public class GreetingService {

	/**
	 * Builds a greeting.
	 *
	 * @param name The name to greet.
	 * @return The greeting text.
	 */
	public String greet(String name) {
		return "Hello, " + name + "! (this greeting came from a Spring-managed bean)";
	}
}
